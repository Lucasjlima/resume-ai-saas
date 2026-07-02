package saas.com.br.resume_ai_saas.resume.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import saas.com.br.resume_ai_saas.resume.entity.Resume;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    /**
     * Every resume for a user, including soft-deleted ones. Used only for
     * administrative full purges (e.g. account deletion), never for API reads.
     */
    List<Resume> findByUserId(UUID userId);

    /**
     * Active (non soft-deleted) resumes for a user, paginated. Backs the
     * owner-scoped listing endpoint.
     */
    Page<Resume> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    /**
     * Owner-scoped single-resume lookup. Returns empty both when the resume does
     * not exist and when it belongs to another user, so callers can respond with
     * a uniform 404 without leaking existence (core IDOR defense).
     */
    Optional<Resume> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    /**
     * Lightweight ownership probe used by {@code @PreAuthorize} checks.
     */
    boolean existsByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
}
