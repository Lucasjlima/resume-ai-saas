package saas.com.br.resume_ai_saas.regeneration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import saas.com.br.resume_ai_saas.analyse.dto.response.Feedback;
import saas.com.br.resume_ai_saas.analyse.entity.Analysis;
import saas.com.br.resume_ai_saas.analyse.exception.AnalysisNotFoundException;
import saas.com.br.resume_ai_saas.analyse.repository.AnalysisRepository;
import saas.com.br.resume_ai_saas.regeneration.dto.response.GeneratedResume;
import saas.com.br.resume_ai_saas.regeneration.entity.RegenerationStatus;
import saas.com.br.resume_ai_saas.regeneration.entity.ResumeRegeneration;
import saas.com.br.resume_ai_saas.regeneration.exception.LatexCompilationException;
import saas.com.br.resume_ai_saas.regeneration.exception.RegenerationNotFoundException;
import saas.com.br.resume_ai_saas.regeneration.exception.RegenerationNotReadyException;
import saas.com.br.resume_ai_saas.regeneration.exception.RegenerationRateLimitExceededException;
import saas.com.br.resume_ai_saas.regeneration.repository.ResumeRegenerationRepository;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.storage.service.ResumeStorageService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegenerationService pipeline, rate limit and ownership chain")
class RegenerationServiceTest {

    private static final int DAILY_LIMIT = 5;
    private static final int MAX_RETRIES = 2;

    @Mock private ResumeRegenerationRepository regenerationRepository;
    @Mock private AnalysisRepository analysisRepository;
    @Mock private AiResumeRegenerationService aiService;
    @Mock private LatexTemplateService latexTemplateService;
    @Mock private LatexCompilationService latexCompilationService;
    @Mock private ResumeStorageService storageService;
    @Mock private Executor executor;
    @Mock private TransactionTemplate transactionTemplate;

    private RegenerationService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID resumeId = UUID.randomUUID();
    private final Long analysisId = 42L;
    private final UUID regenerationId = UUID.randomUUID();

    private Resume resume;
    private Analysis analysis;

    @BeforeEach
    void setUp() {
        service = new RegenerationService(regenerationRepository, analysisRepository, aiService,
                new GeneratedResumeValidator(), latexTemplateService, latexCompilationService,
                storageService, executor, transactionTemplate, DAILY_LIMIT, MAX_RETRIES);

        resume = Resume.builder().id(resumeId).userId(userId).rawText("original resume text").build();
        analysis = Analysis.builder()
                .id(analysisId)
                .resume(resume)
                .feedback(new Feedback(List.of("strong"), List.of("gap"), List.of("improve")))
                .jobDescription("job description")
                .build();

        lenient().when(transactionTemplate.execute(any())).thenAnswer(inv ->
                inv.getArgument(0, TransactionCallback.class).doInTransaction(mock(TransactionStatus.class)));
        lenient().doAnswer(inv -> {
            inv.getArgument(0, Consumer.class).accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private ResumeRegeneration pendingRegeneration() {
        return ResumeRegeneration.builder()
                .id(regenerationId)
                .analysis(analysis)
                .status(RegenerationStatus.PENDING)
                .retryCount(0)
                .attemptNumber(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private GeneratedResume sampleGenerated() {
        return new GeneratedResume(
                new GeneratedResume.DadosPessoais("Maria", "maria@ex.com", null, null, null),
                "Resumo.", List.of(), List.of(), List.of("Java"), List.of(), List.of());
    }

    // --- trigger / rate limit / ownership -----------------------------------

    @Test
    @DisplayName("regenerate: throws when the daily rate limit is reached and saves nothing")
    void regenerate_rateLimited_throwsAndSavesNothing() {
        when(analysisRepository.findById(analysisId)).thenReturn(Optional.of(analysis));
        when(regenerationRepository.countUserAttemptsSince(eq(userId), any())).thenReturn((long) DAILY_LIMIT);

        assertThatThrownBy(() -> service.regenerate(resumeId, analysisId, userId))
                .isInstanceOf(RegenerationRateLimitExceededException.class);

        verify(regenerationRepository, never()).save(any());
    }

    @Test
    @DisplayName("regenerate: computes attempt_number as previous user attempts for the analysis + 1")
    void regenerate_computesAttemptNumber() {
        when(analysisRepository.findById(analysisId)).thenReturn(Optional.of(analysis));
        when(regenerationRepository.countUserAttemptsSince(eq(userId), any())).thenReturn(2L);
        when(regenerationRepository.countByAnalysisId(analysisId)).thenReturn(2);
        when(regenerationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResumeRegeneration created = service.regenerate(resumeId, analysisId, userId);

        assertThat(created.getStatus()).isEqualTo(RegenerationStatus.PENDING);
        assertThat(created.getAttemptNumber()).isEqualTo(3);
        assertThat(created.getRetryCount()).isZero();
    }

    @Test
    @DisplayName("regenerate: analysis belonging to another resume resolves to 404 (no leak)")
    void regenerate_chainMismatch_throwsNotFound() {
        when(analysisRepository.findById(analysisId)).thenReturn(Optional.of(analysis));

        assertThatThrownBy(() -> service.regenerate(UUID.randomUUID(), analysisId, userId))
                .isInstanceOf(AnalysisNotFoundException.class);

        verify(regenerationRepository, never()).save(any());
    }

    // --- async pipeline ------------------------------------------------------

    @Test
    @DisplayName("pipeline: success path ends DONE with the storage path persisted")
    void pipeline_success_marksDone() {
        ResumeRegeneration regeneration = pendingRegeneration();
        when(regenerationRepository.findById(regenerationId)).thenReturn(Optional.of(regeneration));
        when(aiService.regenerate(anyString(), any(), anyString())).thenReturn(sampleGenerated());
        when(latexTemplateService.render(any())).thenReturn("tex");
        when(latexCompilationService.compile("tex")).thenReturn(new byte[]{1});
        when(storageService.uploadRegeneration(eq(userId), eq(regenerationId), any()))
                .thenReturn("resume-regenerations/" + userId + "/" + regenerationId + ".pdf");

        service.processRegenerationAsync(regenerationId);

        assertThat(regeneration.getStatus()).isEqualTo(RegenerationStatus.DONE);
        assertThat(regeneration.getPdfStoragePath())
                .isEqualTo("resume-regenerations/" + userId + "/" + regenerationId + ".pdf");
        assertThat(regeneration.getGeneratedJson()).isNotNull();
        assertThat(regeneration.getRetryCount()).isZero();
        assertThat(regeneration.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("pipeline: AI failure retries automatically without consuming attempt_number")
    void pipeline_aiFailureThenSuccess_retriesWithoutNewAttempt() {
        ResumeRegeneration regeneration = pendingRegeneration();
        when(regenerationRepository.findById(regenerationId)).thenReturn(Optional.of(regeneration));
        when(aiService.regenerate(anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("AI down"))
                .thenReturn(sampleGenerated());
        when(latexTemplateService.render(any())).thenReturn("tex");
        when(latexCompilationService.compile("tex")).thenReturn(new byte[]{1});
        when(storageService.uploadRegeneration(any(), any(), any())).thenReturn("path.pdf");

        service.processRegenerationAsync(regenerationId);

        assertThat(regeneration.getStatus()).isEqualTo(RegenerationStatus.DONE);
        assertThat(regeneration.getRetryCount()).isEqualTo(1);
        assertThat(regeneration.getAttemptNumber()).isEqualTo(1);
        verify(aiService, times(2)).regenerate(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("pipeline: retry after LaTeX failure reuses the persisted generated_json (AI called once)")
    void pipeline_latexFailure_reusesGeneratedJsonOnRetry() {
        ResumeRegeneration regeneration = pendingRegeneration();
        when(regenerationRepository.findById(regenerationId)).thenReturn(Optional.of(regeneration));
        when(aiService.regenerate(anyString(), any(), anyString())).thenReturn(sampleGenerated());
        when(latexTemplateService.render(any())).thenReturn("tex");
        when(latexCompilationService.compile("tex"))
                .thenThrow(new LatexCompilationException("compiler crashed"))
                .thenReturn(new byte[]{1});
        when(storageService.uploadRegeneration(any(), any(), any())).thenReturn("path.pdf");

        service.processRegenerationAsync(regenerationId);

        assertThat(regeneration.getStatus()).isEqualTo(RegenerationStatus.DONE);
        assertThat(regeneration.getRetryCount()).isEqualTo(1);
        verify(aiService, times(1)).regenerate(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("pipeline: after exhausting retries the record is FAILED with failure_reason")
    void pipeline_retriesExhausted_marksFailed() {
        ResumeRegeneration regeneration = pendingRegeneration();
        when(regenerationRepository.findById(regenerationId)).thenReturn(Optional.of(regeneration));
        when(aiService.regenerate(anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("AI permanently down"));

        service.processRegenerationAsync(regenerationId);

        assertThat(regeneration.getStatus()).isEqualTo(RegenerationStatus.FAILED);
        assertThat(regeneration.getFailureReason()).contains("AI permanently down");
        assertThat(regeneration.getRetryCount()).isEqualTo(MAX_RETRIES);
        assertThat(regeneration.getAttemptNumber()).isEqualTo(1);
        verify(aiService, times(MAX_RETRIES + 1)).regenerate(anyString(), any(), anyString());
        verifyNoInteractions(storageService);
    }

    // --- reads / download ----------------------------------------------------

    @Test
    @DisplayName("findByIdForResume: regeneration under another resume resolves to 404 (no leak)")
    void findByIdForResume_chainMismatch_throwsNotFound() {
        ResumeRegeneration regeneration = pendingRegeneration();
        when(regenerationRepository.findById(regenerationId)).thenReturn(Optional.of(regeneration));

        assertThatThrownBy(() -> service.findByIdForResume(regenerationId, UUID.randomUUID()))
                .isInstanceOf(RegenerationNotFoundException.class);
    }

    @Test
    @DisplayName("downloadPdf: refuses while status is not DONE and never touches Storage")
    void downloadPdf_notDone_throwsNotReady() {
        ResumeRegeneration regeneration = pendingRegeneration();
        when(regenerationRepository.findById(regenerationId)).thenReturn(Optional.of(regeneration));

        assertThatThrownBy(() -> service.downloadPdf(regenerationId, resumeId))
                .isInstanceOf(RegenerationNotReadyException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("downloadPdf: serves the stored PDF when DONE")
    void downloadPdf_done_returnsBytes() {
        ResumeRegeneration regeneration = pendingRegeneration();
        regeneration.setStatus(RegenerationStatus.DONE);
        regeneration.setPdfStoragePath("resume-regenerations/" + userId + "/" + regenerationId + ".pdf");
        when(regenerationRepository.findById(regenerationId)).thenReturn(Optional.of(regeneration));
        when(storageService.download(regeneration.getPdfStoragePath())).thenReturn(new byte[]{9});

        byte[] pdf = service.downloadPdf(regenerationId, resumeId);

        assertThat(pdf).containsExactly((byte) 9);
    }

    @Test
    @DisplayName("findByAnalysis: analysis under another resume resolves to 404")
    void findByAnalysis_chainMismatch_throwsNotFound() {
        when(analysisRepository.findById(analysisId)).thenReturn(Optional.of(analysis));

        assertThatThrownBy(() -> service.findByAnalysis(UUID.randomUUID(), analysisId))
                .isInstanceOf(AnalysisNotFoundException.class);

        verify(regenerationRepository, never()).findByAnalysisIdOrderByAttemptNumberAsc(any());
    }
}
