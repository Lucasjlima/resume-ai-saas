package saas.com.br.resume_ai_saas.resume.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.resume.exception.ResumeNotFoundException;
import saas.com.br.resume_ai_saas.resume.exception.UserNotFoundException;
import saas.com.br.resume_ai_saas.resume.mapper.ResumeMapper;
import saas.com.br.resume_ai_saas.resume.repository.ResumeRepository;
import saas.com.br.resume_ai_saas.user.entity.User;
import saas.com.br.resume_ai_saas.user.repository.UserRepository;

import java.util.List;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final ResumeTextExtractionService textExtractionService;
    private final ResumeService self;

    public ResumeService(ResumeRepository resumeRepository,
                         UserRepository userRepository,
                         StorageService storageService,
                         ResumeTextExtractionService textExtractionService,
                         @Lazy ResumeService self) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.textExtractionService = textExtractionService;
        this.self = self;
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

    public Resume upload(Long userId, MultipartFile file) {
        String fileUrl = storageService.store(userId, file);
        String rawText = textExtractionService.extract(file);

        return self.saveResume(userId, file.getOriginalFilename(), fileUrl, rawText);
    }

    @Transactional
    public Resume saveResume(Long userId, String fileName, String fileUrl, String rawText) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Resume resume = ResumeMapper.toEntity(user, fileName, fileUrl, rawText);
        return resumeRepository.save(resume);
    }

    @Transactional
    public void delete(Long id) {
        if (!resumeRepository.existsById(id)) {
            throw new ResumeNotFoundException(id);
        }
        resumeRepository.deleteById(id);
    }
}
