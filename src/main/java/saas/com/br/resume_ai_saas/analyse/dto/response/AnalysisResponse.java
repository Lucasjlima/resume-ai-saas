package saas.com.br.resume_ai_saas.analyse.dto.response;

import saas.com.br.resume_ai_saas.analyse.entity.AnalysisStatus;

import java.time.Instant;
import java.util.UUID;

public record AnalysisResponse(
    Long id,
    UUID resumeId,
    Integer overallScore,
    Feedback feedback,
    String jobDescription,
    AnalysisStatus status,
    Instant createdAt
) {}
