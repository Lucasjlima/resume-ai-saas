package saas.com.br.resume_ai_saas.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.com.br.resume_ai_saas.resume.repository.ResumeRepository;

import java.util.UUID;

/**
 * Ownership authorization component used by Spring Security method expressions
 * (e.g. {@code @PreAuthorize("@resumeSecurity.isOwner(#id)")}).
 *
 * <p>The application connects to Postgres with administrative credentials and
 * therefore bypasses any database RLS. All IDOR protection must run here, in the
 * application layer, by comparing the {@code userId} extracted from the verified
 * JWT against the owner of the target resume.
 */
@Component("resumeSecurity")
@RequiredArgsConstructor
public class ResumeSecurity {

    private final ResumeRepository resumeRepository;

    /**
     * @return {@code true} only when the currently authenticated user owns an
     * active (non soft-deleted) resume with the given id. Returns {@code false}
     * for missing resumes, soft-deleted resumes, and resumes owned by another
     * user — the {@code @PreAuthorize} gate then rejects the request before the
     * controller method (and any downstream Storage/S3 call) executes.
     */
    public boolean isOwner(UUID resumeId) {
        if (resumeId == null) {
            return false;
        }
        UUID userId = AuthenticatedUser.getId();
        return resumeRepository.existsByIdAndUserIdAndDeletedAtIsNull(resumeId, userId);
    }
}
