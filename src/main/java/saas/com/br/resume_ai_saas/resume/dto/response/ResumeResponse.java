package saas.com.br.resume_ai_saas.resume.dto.response;

import saas.com.br.resume_ai_saas.resume.entity.ExtractionMethod;
import java.time.Instant;

public record ResumeResponse(
    Long id,
    Long userId,
    String fileName,
    String fileUrl,
    String rawText,
    ExtractionMethod extractionMethod,
    Instant createdAt
) {}
