package saas.com.br.resume_ai_saas.analyse.dto;

import saas.com.br.resume_ai_saas.analyse.entity.AnalysisStatus;
import java.time.Instant;

public record AnalysisResponse(
    Long id,
    Long resumeId,
    Integer overallScore,
    String feedbackJson,
    String jobDescription,
    AnalysisStatus status,
    Instant createdAt
) {}
