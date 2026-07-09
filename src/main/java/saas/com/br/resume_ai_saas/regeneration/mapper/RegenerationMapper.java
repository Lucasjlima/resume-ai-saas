package saas.com.br.resume_ai_saas.regeneration.mapper;

import saas.com.br.resume_ai_saas.analyse.entity.Analysis;
import saas.com.br.resume_ai_saas.regeneration.dto.response.RegenerationResponse;
import saas.com.br.resume_ai_saas.regeneration.entity.RegenerationStatus;
import saas.com.br.resume_ai_saas.regeneration.entity.ResumeRegeneration;

import java.time.Instant;

public final class RegenerationMapper {

    private RegenerationMapper() {}

    public static ResumeRegeneration toPendingRegeneration(Analysis analysis, int attemptNumber) {
        Instant now = Instant.now();
        return ResumeRegeneration.builder()
                .analysis(analysis)
                .status(RegenerationStatus.PENDING)
                .retryCount(0)
                .attemptNumber(attemptNumber)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static RegenerationResponse toResponse(ResumeRegeneration regeneration) {
        return new RegenerationResponse(
                regeneration.getId(),
                regeneration.getAnalysis().getId(),
                regeneration.getStatus(),
                regeneration.getAttemptNumber(),
                regeneration.getRetryCount(),
                regeneration.getFailureReason(),
                regeneration.getCreatedAt(),
                regeneration.getUpdatedAt()
        );
    }
}
