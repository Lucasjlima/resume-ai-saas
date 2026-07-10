package saas.com.br.resume_ai_saas.regeneration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StopWatch;
import saas.com.br.resume_ai_saas.analyse.dto.response.Feedback;
import saas.com.br.resume_ai_saas.analyse.entity.Analysis;
import saas.com.br.resume_ai_saas.analyse.exception.AnalysisNotFoundException;
import saas.com.br.resume_ai_saas.analyse.repository.AnalysisRepository;
import saas.com.br.resume_ai_saas.regeneration.dto.response.GeneratedResume;
import saas.com.br.resume_ai_saas.regeneration.entity.RegenerationStatus;
import saas.com.br.resume_ai_saas.regeneration.entity.ResumeRegeneration;
import saas.com.br.resume_ai_saas.regeneration.exception.RegenerationNotFoundException;
import saas.com.br.resume_ai_saas.regeneration.exception.RegenerationNotReadyException;
import saas.com.br.resume_ai_saas.regeneration.exception.RegenerationRateLimitExceededException;
import saas.com.br.resume_ai_saas.regeneration.mapper.RegenerationMapper;
import saas.com.br.resume_ai_saas.regeneration.repository.ResumeRegenerationRepository;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.storage.service.ResumeStorageService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class RegenerationService {

    private final ResumeRegenerationRepository regenerationRepository;
    private final AnalysisRepository analysisRepository;
    private final AiResumeRegenerationService aiService;
    private final GeneratedResumeValidator validator;
    private final LatexTemplateService latexTemplateService;
    private final LatexCompilationService latexCompilationService;
    private final PdfPageCounter pdfPageCounter;
    private final ResumeStorageService storageService;
    private final Executor executor;
    private final TransactionTemplate transactionTemplate;
    private final int dailyRateLimit;
    private final int maxRetries;

    public RegenerationService(ResumeRegenerationRepository regenerationRepository,
                               AnalysisRepository analysisRepository,
                               AiResumeRegenerationService aiService,
                               GeneratedResumeValidator validator,
                               LatexTemplateService latexTemplateService,
                               LatexCompilationService latexCompilationService,
                               PdfPageCounter pdfPageCounter,
                               ResumeStorageService storageService,
                               @Qualifier("applicationTaskExecutor") Executor executor,
                               TransactionTemplate transactionTemplate,
                               @Value("${regeneration.rate-limit.daily:5}") int dailyRateLimit,
                               @Value("${regeneration.max-retries:2}") int maxRetries) {
        this.regenerationRepository = regenerationRepository;
        this.analysisRepository = analysisRepository;
        this.aiService = aiService;
        this.validator = validator;
        this.latexTemplateService = latexTemplateService;
        this.latexCompilationService = latexCompilationService;
        this.pdfPageCounter = pdfPageCounter;
        this.storageService = storageService;
        this.executor = executor;
        this.transactionTemplate = transactionTemplate;
        this.dailyRateLimit = dailyRateLimit;
        this.maxRetries = maxRetries;
    }

    public ResumeRegeneration regenerate(UUID resumeId, Long analysisId, UUID userId) {
        ResumeRegeneration regeneration = initializeRegeneration(resumeId, analysisId, userId);
        final UUID regenerationId = regeneration.getId();
        CompletableFuture.runAsync(() -> processRegenerationAsync(regenerationId), executor);
        return regeneration;
    }

    @Transactional(readOnly = true)
    public ResumeRegeneration findByIdForResume(UUID regenerationId, UUID resumeId) {
        ResumeRegeneration regeneration = regenerationRepository.findById(regenerationId)
                .orElseThrow(() -> new RegenerationNotFoundException(regenerationId));
        if (!regeneration.getAnalysis().getResume().getId().equals(resumeId)) {
            throw new RegenerationNotFoundException(regenerationId);
        }
        return regeneration;
    }

    @Transactional(readOnly = true)
    public List<ResumeRegeneration> findByAnalysis(UUID resumeId, Long analysisId) {
        requireAnalysisOfResume(analysisId, resumeId);
        return regenerationRepository.findByAnalysisIdOrderByAttemptNumberAsc(analysisId);
    }

    @Transactional(readOnly = true)
    public byte[] downloadPdf(UUID regenerationId, UUID resumeId) {
        ResumeRegeneration regeneration = findByIdForResume(regenerationId, resumeId);
        if (regeneration.getStatus() != RegenerationStatus.DONE || regeneration.getPdfStoragePath() == null) {
            throw new RegenerationNotReadyException(regenerationId);
        }
        return storageService.download(regeneration.getPdfStoragePath());
    }

    private ResumeRegeneration initializeRegeneration(UUID resumeId, Long analysisId, UUID userId) {
        return transactionTemplate.execute(status -> {
            Analysis analysis = requireAnalysisOfResume(analysisId, resumeId);

            long attemptsInWindow = regenerationRepository
                    .countUserAttemptsSince(userId, Instant.now().minus(Duration.ofHours(24)));
            if (attemptsInWindow >= dailyRateLimit) {
                throw new RegenerationRateLimitExceededException(dailyRateLimit);
            }

            int attemptNumber = regenerationRepository.countByAnalysisId(analysisId) + 1;
            return regenerationRepository.save(RegenerationMapper.toPendingRegeneration(analysis, attemptNumber));
        });
    }

    private Analysis requireAnalysisOfResume(Long analysisId, UUID resumeId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));
        if (!analysis.getResume().getId().equals(resumeId)) {
            throw new AnalysisNotFoundException(analysisId);
        }
        return analysis;
    }

    /**
     * Runs the AI → LaTeX → Storage pipeline, retrying automatically on system
     * failures inside the same record ({@code retry_count}); user-facing
     * {@code attempt_number} never changes here.
     */
    public void processRegenerationAsync(UUID regenerationId) {
        log.info("[Regeneração {}] Iniciando processamento assíncrono...", regenerationId);
        while (true) {
            try {
                runPipelineOnce(regenerationId);
                log.info("[Regeneração {}] Finalizada com SUCESSO", regenerationId);
                return;
            } catch (Exception e) {
                boolean willRetry = registerFailure(regenerationId, e);
                if (!willRetry) {
                    return;
                }
            }
        }
    }

    private void runPipelineOnce(UUID regenerationId) {
        StopWatch stopWatch = new StopWatch("Regeneration Pipeline - ID: " + regenerationId);
        PipelineContext context = markProcessing(regenerationId);

        GeneratedResume generated = context.generatedJson();
        if (generated == null) {
            stopWatch.start("AI Generation");
            generated = validator.validateAndNormalize(
                    aiService.regenerate(context.resumeText(), context.feedback(), context.jobDescription()));
            stopWatch.stop();

            stopWatch.start("Persist generated_json");
            persistGeneratedJson(regenerationId, generated);
            stopWatch.stop();
        } else {
            log.info("[Regeneração {}] Reutilizando generated_json persistido — IA não será chamada",
                    regenerationId);
        }

        stopWatch.start("LaTeX Template Render");
        String texSource = latexTemplateService.render(generated, false);
        stopWatch.stop();

        stopWatch.start("LaTeX Compilation");
        byte[] pdfBytes = latexCompilationService.compile(texSource);
        stopWatch.stop();

        int pages = pdfPageCounter.count(pdfBytes);
        if (pages > 1) {
            log.info("[Regeneração {}] PDF saiu com {} páginas — recompilando em layout compacto",
                    regenerationId, pages);
            stopWatch.start("Compact Recompilation");
            pdfBytes = latexCompilationService.compile(latexTemplateService.render(generated, true));
            stopWatch.stop();

            int compactPages = pdfPageCounter.count(pdfBytes);
            if (compactPages > 1) {
                log.warn("[Regeneração {}] Ainda com {} páginas mesmo no layout compacto — conteúdo extenso demais",
                        regenerationId, compactPages);
            }
        }

        stopWatch.start("Storage Upload");
        String storagePath = storageService.uploadRegeneration(context.userId(), regenerationId, pdfBytes);
        stopWatch.stop();

        markDone(regenerationId, storagePath);
        log.info("[Regeneração {}] Pipeline concluído em {} s\n{}",
                regenerationId, stopWatch.getTotalTimeSeconds(), stopWatch.prettyPrint());
    }

    private PipelineContext markProcessing(UUID regenerationId) {
        return transactionTemplate.execute(status -> {
            ResumeRegeneration regeneration = loadOrThrow(regenerationId);
            regeneration.setStatus(RegenerationStatus.PROCESSING);
            regeneration.setUpdatedAt(Instant.now());
            regenerationRepository.save(regeneration);

            Resume resume = regeneration.getAnalysis().getResume();
            return new PipelineContext(resume.getUserId(), resume.getRawText(),
                    regeneration.getAnalysis().getFeedback(), regeneration.getAnalysis().getJobDescription(),
                    regeneration.getGeneratedJson());
        });
    }

    private void persistGeneratedJson(UUID regenerationId, GeneratedResume generated) {
        transactionTemplate.executeWithoutResult(status -> {
            ResumeRegeneration regeneration = loadOrThrow(regenerationId);
            regeneration.setGeneratedJson(generated);
            regeneration.setUpdatedAt(Instant.now());
            regenerationRepository.save(regeneration);
        });
    }

    private void markDone(UUID regenerationId, String storagePath) {
        transactionTemplate.executeWithoutResult(status -> {
            ResumeRegeneration regeneration = loadOrThrow(regenerationId);
            regeneration.setStatus(RegenerationStatus.DONE);
            regeneration.setPdfStoragePath(storagePath);
            regeneration.setUpdatedAt(Instant.now());
            regenerationRepository.save(regeneration);
        });
    }

    private boolean registerFailure(UUID regenerationId, Exception error) {
        Boolean willRetry = transactionTemplate.execute(status -> {
            ResumeRegeneration regeneration = loadOrThrow(regenerationId);
            regeneration.setUpdatedAt(Instant.now());
            if (regeneration.getRetryCount() < maxRetries) {
                regeneration.setRetryCount(regeneration.getRetryCount() + 1);
                regenerationRepository.save(regeneration);
                return true;
            }
            regeneration.setStatus(RegenerationStatus.FAILED);
            regeneration.setFailureReason(error.getMessage());
            regenerationRepository.save(regeneration);
            return false;
        });

        if (Boolean.TRUE.equals(willRetry)) {
            log.warn("[Regeneração {}] Falha, tentando novamente automaticamente: {}",
                    regenerationId, error.getMessage());
            return true;
        }
        log.error("[Regeneração {}] Falha definitiva após esgotar retries automáticos", regenerationId, error);
        return false;
    }

    private ResumeRegeneration loadOrThrow(UUID regenerationId) {
        return regenerationRepository.findById(regenerationId)
                .orElseThrow(() -> new RegenerationNotFoundException(regenerationId));
    }

    private record PipelineContext(UUID userId, String resumeText, Feedback feedback,
                                   String jobDescription, GeneratedResume generatedJson) {}
}
