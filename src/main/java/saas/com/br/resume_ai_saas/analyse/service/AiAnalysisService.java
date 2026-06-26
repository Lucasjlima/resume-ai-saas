package saas.com.br.resume_ai_saas.analyse.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Service;
import saas.com.br.resume_ai_saas.analyse.dto.response.AiAnalysisResult;

@Service
public class AiAnalysisService {

    private final ChatClient chatClient;

    public AiAnalysisService(ChatClient.Builder chatClientBuilder) {
        GoogleGenAiChatOptions.Builder options = GoogleGenAiChatOptions.builder()
                .model("gemini-2.5-flash")
                .temperature(0.1);

        this.chatClient = chatClientBuilder
                .defaultOptions(options)
                .build();
    }

    public AiAnalysisResult analyze(String resumeText, String jobDescription) {
        var outputConverter = new BeanOutputConverter<>(AiAnalysisResult.class);

        String promptTemplate = """
                You are an expert ATS (Applicant Tracking System) and resume evaluator.
                Analyze the resume against the job description provided.

                SCORING INSTRUCTIONS — follow exactly in this order:

                Step 1 — Score each criterion independently from 0 to 100:
                  - hardSkillsScore:  Technical skills, tools, and methodologies match.
                  - experienceScore:  Years of experience, roles, and responsibilities match.
                  - softSkillsScore:  Action verbs, communication, and ATS keyword optimization.
                  - educationScore:   Required degrees or certifications match.

                Step 2 — Calculate overallScore using this exact formula:
                  overallScore = round((hardSkillsScore * 0.35) + (experienceScore * 0.35)
                                      + (softSkillsScore * 0.20) + (educationScore * 0.10))

                Do NOT estimate overallScore freely. Always derive it from the formula above.

                OUTPUT RULES:
                - Respond in the SAME LANGUAGE used in the Resume.
                - If the resume is invalid or unrelated to a professional profile,
                  set all scores to 0 and explain in "improvements".

                RESUME:
                {resumeText}

                JOB DESCRIPTION:
                {jobDescription}

                {format}
                """;

        AiAnalysisResult result = chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(promptTemplate)
                        .param("resumeText", resumeText)
                        .param("jobDescription", jobDescription)
                        .param("format", outputConverter.getFormat())
                )
                .call()
                .entity(outputConverter);

        // Validação: garante que o overallScore bate com a fórmula
        int expectedScore = (int) Math.round(
            (result.hardSkillsScore() * 0.35) +
            (result.experienceScore() * 0.35) +
            (result.softSkillsScore() * 0.20) +
            (result.educationScore() * 0.10)
        );

        if (Math.abs(result.overallScore() - expectedScore) > 2) {
            result = new AiAnalysisResult(
                expectedScore,
                result.hardSkillsScore(),
                result.experienceScore(),
                result.softSkillsScore(),
                result.educationScore(),
                result.feedback()
            );
        }

        return result;
    }
}
