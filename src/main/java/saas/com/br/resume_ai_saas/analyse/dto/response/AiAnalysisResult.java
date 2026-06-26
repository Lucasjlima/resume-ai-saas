package saas.com.br.resume_ai_saas.analyse.dto.response;

public record AiAnalysisResult(
    int overallScore,
    int hardSkillsScore,
    int experienceScore,
    int softSkillsScore,
    int educationScore,
    Feedback feedback
) {}
