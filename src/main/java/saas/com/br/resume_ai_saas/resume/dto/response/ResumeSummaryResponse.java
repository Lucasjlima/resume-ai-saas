package saas.com.br.resume_ai_saas.resume.dto.response;

import saas.com.br.resume_ai_saas.resume.entity.ExtractionMethod;
import saas.com.br.resume_ai_saas.resume.entity.ResumeStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight listing DTO returned by the paginated {@code GET /api/resumes}
 * endpoint. Intentionally omits the full extracted text and any internal
 * Storage keys — list views only need metadata.
 */
public record ResumeSummaryResponse(
        UUID id,
        String fileName,
        ResumeStatus status,
        ExtractionMethod extractionMethod,
        Instant createdAt
) {}
