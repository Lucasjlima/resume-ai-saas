package saas.com.br.resume_ai_saas.analyse.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import saas.com.br.resume_ai_saas.analyse.dto.response.AiAnalysisResult;

@Service
public class AiAnalysisService {

    private final ChatClient chatClient;

    public AiAnalysisService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public AiAnalysisResult analyze(String resumeText, String jobDescription) {
        var outputConverter = new BeanOutputConverter<>(AiAnalysisResult.class);

        String promptTemplate = """
                You are an expert ATS (Applicant Tracking System) and resume evaluator.
                Analyze the following resume against the job description provided.

                CRITERIA FOR OVERALL SCORE (0-100):
                - Hard Skills Match (35%): Technical skills, tools, and methodologies.
                - Experience Match (35%): Years of experience, roles, and responsibilities.
                - Soft Skills & Keywords (20%): Action verbs, communication, and ATS optimization.
                - Education & Certifications (10%): Required degrees or certifications.

                OUTPUT RULES:
                - Respond in the SAME LANGUAGE used in the Resume.
                - If the Resume text is invalid, empty, or completely unrelated to a professional profile, set "overallScore" to 0 and add a single message in "improvements" explaining the issue.

                RESUME:
                {resumeText}

                JOB DESCRIPTION:
                {jobDescription}

                {format}
                """;

        return chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(promptTemplate)
                        .param("resumeText", resumeText)
                        .param("jobDescription", jobDescription)
                        .param("format", outputConverter.getFormat())
                )
                .call()
                .entity(outputConverter);
    }
}
