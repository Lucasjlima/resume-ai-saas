package saas.com.br.resume_ai_saas.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.resume.entity.ResumeStatus;
import saas.com.br.resume_ai_saas.resume.exception.ResumeNotFoundException;
import saas.com.br.resume_ai_saas.resume.exception.UserNotFoundException;
import saas.com.br.resume_ai_saas.resume.mapper.ResumeMapper;
import saas.com.br.resume_ai_saas.resume.repository.ResumeRepository;
import saas.com.br.resume_ai_saas.user.entity.User;
import saas.com.br.resume_ai_saas.user.repository.UserRepository;

import java.util.List;

@Service
@Slf4j
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final ResumeTextExtractionService textExtractionService;
    private final ResumeTextRefinementService textRefinementService;
    private final ResumeService self;

    public ResumeService(ResumeRepository resumeRepository,
                         UserRepository userRepository,
                         StorageService storageService,
                         ResumeTextExtractionService textExtractionService,
                         ResumeTextRefinementService textRefinementService,
                         @Lazy ResumeService self) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.textExtractionService = textExtractionService;
        this.textRefinementService = textRefinementService;
        this.self = self;
    }


    /**
     * Sychronous entry point. Fast file storage and meta persistence.
     * Drops API response time from ~20s to < 1s.
     */
    public Resume upload(Long userId, MultipartFile file) {
        // Read file bytes synchronously to prevent Tomcat from cleaning up temporary file before async execution
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read uploaded file bytes", e);
        }
        String originalFilename = file.getOriginalFilename();

        // 1. Persist file physically first to ensure availability for processing
        String fileUrl = storageService.store(userId, file);

        // 2. Persist metadata into DB with standard 'PROCESSING' state
        Resume resume = self.saveInitialResume(userId, originalFilename, fileUrl);

        // 3. Delegate heavy processing asynchronously via proxied self bean reference
        self.processResumeBackground(resume.getId(), fileBytes, originalFilename);

        return resume;
    }

    @Transactional
    public Resume saveInitialResume(Long userId, String fileName, String fileUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Ensure mapper creates entity with 'ResumeStatus.PROCESSING' or similar initial state
        Resume resume = ResumeMapper.toEntity(user, fileName, fileUrl, "Extracting and refining content...");
        resume.setStatus(ResumeStatus.PROCESSING);

        return resumeRepository.save(resume);
    }

    /**
     * Non-blocking background worker task executing text processing and AI querying.
     */
    @Async
    public void processResumeBackground(Long resumeId, byte[] fileBytes, String originalFilename) {
        log.info("Starting async background processing for Resume ID: {}", resumeId);
        StopWatch stopWatch = new StopWatch("Resume Processing Metrics - ID: " + resumeId);

        try {
            // Task 1: Document Text Extraction
            stopWatch.start("Text Extraction");
            String rawText = textExtractionService.extract(fileBytes, originalFilename);
            stopWatch.stop();

            // Task 2: LLM Prompting via Spring AI ChatClient
            stopWatch.start("AI Text Refinement");
            String refinedText = textRefinementService.refine(rawText);
            stopWatch.stop();

            // Task 3: Final State Database Persistence
            stopWatch.start("Database Update & Persistence");
            self.updateRefinedText(resumeId, refinedText, ResumeStatus.COMPLETED);
            stopWatch.stop();

            log.info("Async processing completed successfully!\n{}", stopWatch.prettyPrint());

        } catch (Exception e) {
            log.error("Failed to complete async resume processing for ID: {}", resumeId, e);
            try {
                self.updateRefinedText(resumeId, null, ResumeStatus.FAILED);
            } catch (Exception ex) {
                log.error("Failed to mark resume status as FAILED in DB for ID: {}", resumeId, ex);
            }
        }
    }

    @Transactional
    public void updateRefinedText(Long resumeId, String refinedText, ResumeStatus status) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResumeNotFoundException(resumeId));

        if (refinedText != null) {
            resume.setRawText(refinedText);
        }
        resume.setStatus(status);
        resumeRepository.save(resume);
    }

    @Transactional(readOnly = true)
    public List<Resume> findByUserId(Long userId) {
        return resumeRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Resume findById(Long id) {
        return resumeRepository.findById(id)
                .orElseThrow(() -> new ResumeNotFoundException(id));
    }

    @Transactional
    public void delete(Long id) {
        if (!resumeRepository.existsById(id)) {
            throw new ResumeNotFoundException(id);
        }
        resumeRepository.deleteById(id);
    }
}
