package saas.com.br.resume_ai_saas.regeneration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import saas.com.br.resume_ai_saas.regeneration.entity.ResumeRegeneration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ResumeRegenerationRepository extends JpaRepository<ResumeRegeneration, UUID> {

    List<ResumeRegeneration> findByAnalysisIdOrderByAttemptNumberAsc(Long analysisId);

    int countByAnalysisId(Long analysisId);

    @Query("""
            select count(r) from ResumeRegeneration r
            where r.analysis.resume.userId = :userId
              and r.createdAt >= :since
            """)
    long countUserAttemptsSince(@Param("userId") UUID userId, @Param("since") Instant since);

    @Query("""
            select r.pdfStoragePath from ResumeRegeneration r
            where r.analysis.resume.id = :resumeId
              and r.pdfStoragePath is not null
            """)
    List<String> findPdfStoragePathsByResumeId(@Param("resumeId") UUID resumeId);
}
