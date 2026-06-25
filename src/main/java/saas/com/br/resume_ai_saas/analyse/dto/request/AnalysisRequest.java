package saas.com.br.resume_ai_saas.analyse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalysisRequest(
    @NotBlank(message = "Job description is required")
    @Size(min = 50, max = 5000, message = "Job description must be between 50 and 5000 characters")
    String jobDescription
) {}
