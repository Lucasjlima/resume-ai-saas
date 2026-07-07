package saas.com.br.resume_ai_saas.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.com.br.resume_ai_saas.resume.repository.ResumeRepository;

import java.util.UUID;

@Component("resumeSecurity")
@RequiredArgsConstructor
public class ResumeSecurity {

    private final ResumeRepository resumeRepository;

    public boolean isOwner(UUID resumeId) {
        if (resumeId == null) {
            return false;
        }
        UUID userId = AuthenticatedUser.getId();
        return resumeRepository.existsByIdAndUserId(resumeId, userId);
    }
}
