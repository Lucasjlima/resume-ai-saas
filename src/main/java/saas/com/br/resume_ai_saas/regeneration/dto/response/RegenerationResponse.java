package saas.com.br.resume_ai_saas.regeneration.dto.response;

import saas.com.br.resume_ai_saas.regeneration.entity.RegenerationStatus;

import java.time.Instant;
import java.util.UUID;

public record RegenerationResponse(
        UUID id,
        Long analysisId,
        RegenerationStatus status,
        int attemptNumber,
        int retryCount,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {}
