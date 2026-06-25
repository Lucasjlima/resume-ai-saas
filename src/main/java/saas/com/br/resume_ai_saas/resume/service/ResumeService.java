package saas.com.br.resume_ai_saas.resume.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.com.br.resume_ai_saas.resume.entity.ExtractionMethod;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.resume.exception.ResumeNotFoundException;
import saas.com.br.resume_ai_saas.resume.exception.UserNotFoundException;
import saas.com.br.resume_ai_saas.resume.repository.ResumeRepository;
import saas.com.br.resume_ai_saas.user.entity.User;
import saas.com.br.resume_ai_saas.user.repository.UserRepository;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final ResumeTextExtractionService textExtractionService;

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
    public Resume upload(Long userId, org.springframework.web.multipart.MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String fileUrl = storageService.store(userId, file);
        String rawText = textExtractionService.extract(file);

        Resume resume = Resume.builder()
                .user(user)
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .rawText(rawText)
                .extractionMethod(ExtractionMethod.PDF_PARSER)
                .createdAt(Instant.now())
                .build();

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
