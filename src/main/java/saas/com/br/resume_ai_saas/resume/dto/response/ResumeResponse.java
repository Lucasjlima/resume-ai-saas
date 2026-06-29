package saas.com.br.resume_ai_saas.resume.dto.response;

import saas.com.br.resume_ai_saas.resume.entity.ExtractionMethod;

import java.time.Instant;
import java.util.UUID;

public record ResumeResponse(
    Long id,
    UUID userId,
    String fileName,
    String fileUrl,
    String rawText,
    ExtractionMethod extractionMethod,
    Instant createdAt
) {}
