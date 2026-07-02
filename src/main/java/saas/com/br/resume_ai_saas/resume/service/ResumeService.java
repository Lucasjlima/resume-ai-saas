package saas.com.br.resume_ai_saas.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;
import saas.com.br.resume_ai_saas.exception.ResumeNotFoundException;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.resume.entity.ResumeStatus;
import saas.com.br.resume_ai_saas.resume.mapper.ResumeMapper;
import saas.com.br.resume_ai_saas.resume.repository.ResumeRepository;
import saas.com.br.resume_ai_saas.storage.service.ResumeStorageService;
import org.springframework.security.access.AccessDeniedException;
import saas.com.br.resume_ai_saas.security.AuthenticatedUser;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


@Service
@Slf4j
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeStorageService storageService;
    private final ResumeTextExtractionService textExtractionService;
    private final ResumeTextRefinementService textRefinementService;
    private final Executor executor;
    private final TransactionTemplate transactionTemplate;

    public ResumeService(ResumeRepository resumeRepository,
                         ResumeStorageService storageService,
                         ResumeTextExtractionService textExtractionService,
                         ResumeTextRefinementService textRefinementService,
                         @Qualifier("applicationTaskExecutor") Executor executor,
                         TransactionTemplate transactionTemplate) {
        this.resumeRepository = resumeRepository;
        this.storageService = storageService;
        this.textExtractionService = textExtractionService;
        this.textRefinementService = textRefinementService;
        this.executor = executor;
        this.transactionTemplate = transactionTemplate;
    }

    public Resume upload(UUID userId, MultipartFile file) {
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read uploaded file bytes", e);
        }
        String originalFilename = file.getOriginalFilename();

        String rawText = textExtractionService.extract(fileBytes, originalFilename);

        Resume resume = ResumeMapper.toEntity(userId, originalFilename, rawText);
        resume = resumeRepository.save(resume);

        try {
            String storageKey = storageService.upload(userId, resume.getId(), fileBytes);
            resume.setStorageKey(storageKey);
            resume = resumeRepository.save(resume);
        } catch (Exception e) {
            log.error("Failed to upload resume file to storage for Resume ID: {}", resume.getId(), e);
            resume.setStatus(ResumeStatus.FILE_UPLOAD_FAILED);
            resume = resumeRepository.save(resume);
        }

        final UUID resumeId = resume.getId();
        final String rawTextToRefine = rawText;
        CompletableFuture.runAsync(() -> processResumeBackground(resumeId, rawTextToRefine), executor);

        return resume;
    }

    public void processResumeBackground(UUID resumeId, String rawText) {
        log.info("Starting async background refinement for Resume ID: {}", resumeId);
        StopWatch stopWatch = new StopWatch("Resume Refinement Metrics - ID: " + resumeId);

        try {
            stopWatch.start("AI Text Refinement");
            String refinedText = textRefinementService.refine(rawText);
            stopWatch.stop();

            stopWatch.start("Database Update & Persistence");
            updateRefinedText(resumeId, refinedText, ResumeStatus.COMPLETED);
            stopWatch.stop();

            log.info("Async refinement completed successfully!\n{}", stopWatch.prettyPrint());

        } catch (Exception e) {
            log.error("Failed to complete async resume refinement for ID: {}", resumeId, e);
            try {
                updateRefinedText(resumeId, null, ResumeStatus.FAILED);
            } catch (Exception ex) {
                log.error("Failed to mark resume status as FAILED in DB for ID: {}", resumeId, ex);
            }
        }
    }

    public void updateRefinedText(UUID resumeId, String refinedText, ResumeStatus status) {
        transactionTemplate.executeWithoutResult(ts -> {
            Resume resume = resumeRepository.findById(resumeId)
                    .orElseThrow(() -> new ResumeNotFoundException(resumeId));
            if (refinedText != null) {
                resume.setRawText(refinedText);
            }
            if (resume.getStatus() != ResumeStatus.FILE_UPLOAD_FAILED) {
                resume.setStatus(status);
            }
            resumeRepository.save(resume);
        });
    }

    @Transactional(readOnly = true)
    public List<Resume> findByUserId(UUID userId) {
        return resumeRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Resume findById(UUID id) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResumeNotFoundException(id));
        UUID currentUserId = AuthenticatedUser.getId();
        if (!resume.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this resume");
        }
        return resume;
    }

    public byte[] downloadRawPdf(UUID id) {
        Resume resume = findById(id);
        if (resume.getStorageKey() == null) {
            throw new IllegalStateException("Resume has no stored file: " + id);
        }
        return storageService.download(resume.getStorageKey());
    }

    public String generatePresignedDownloadUrl(UUID id, Duration ttl) {
        Resume resume = findById(id);
        if (resume.getStorageKey() == null) {
            throw new IllegalStateException("Resume has no stored file: " + id);
        }
        return storageService.generatePresignedDownloadUrl(resume.getStorageKey(), ttl);
    }

    @Transactional
    public void delete(UUID id) {
        Resume resume = findById(id);
        if (resume.getStorageKey() != null) {
            try {
                storageService.delete(resume.getStorageKey());
            } catch (Exception e) {
                log.error("Failed to delete storage object for Resume ID: {} (key={})",
                        id, resume.getStorageKey(), e);
                throw e;
            }
        }
        resumeRepository.delete(resume);
    }

    @Transactional
    public void deleteAllForUser(UUID userId) {
        storageService.deleteAllForUser(userId);
        List<Resume> resumes = resumeRepository.findByUserId(userId);
        resumeRepository.deleteAll(resumes);
    }
}
