package saas.com.br.resume_ai_saas.resume.dto.response;

import saas.com.br.resume_ai_saas.resume.entity.ExtractionMethod;
import saas.com.br.resume_ai_saas.resume.entity.ResumeStatus;

import java.time.Instant;
import java.util.UUID;

public record ResumeResponse(
    UUID id,
    UUID userId,
    String fileName,
    String rawText,
    ResumeStatus status,
    ExtractionMethod extractionMethod,
    Instant createdAt
) {}
