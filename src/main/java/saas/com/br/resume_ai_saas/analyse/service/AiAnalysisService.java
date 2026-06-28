package saas.com.br.resume_ai_saas.analyse.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Service;
import saas.com.br.resume_ai_saas.analyse.dto.response.AiAnalysisResult;

@Service
public class AiAnalysisService {

    private final ChatClient chatClient;
    private final BeanOutputConverter<AiAnalysisResult> outputConverter =
            new BeanOutputConverter<>(AiAnalysisResult.class);

    public AiAnalysisService(ChatClient.Builder chatClientBuilder) {
        GoogleGenAiChatOptions.Builder options = GoogleGenAiChatOptions.builder()
                .model("gemini-2.5-flash")
                .responseMimeType("application/json")
                .temperature(0.1);

        this.chatClient = chatClientBuilder
                .defaultOptions(options)
                .build();
    }

    public AiAnalysisResult analyze(String resumeText, String jobDescription) {
        int currentYear = java.time.Year.now().getValue();

        String promptTemplate = """
                You are an expert ATS (Applicant Tracking System) and resume evaluator.
                Analyze the resume against the job description provided.
                
                CURRENT YEAR CONTEXT: The current year is {currentYear}. 
                If you find dates in the resume that seem mismatched (like future dates or minor typos), DO NOT invalidate the resume. Assume it is a typo, evaluate the technical content normally, but mention it quickly in the gaps.
                
                EVALUATION CRITERIA — Rate each from 0 to 100 based on the match:
                  - hardSkillsScore:  Technical skills, tools, frameworks, and methodologies match.
                  - experienceScore:  Years of experience, previous roles, and responsibilities match.
                  - softSkillsScore:  Action verbs, professional communication, and ATS keyword optimization.
                  - educationScore:   Required degrees, academic background, or certifications match.
                
                FEEDBACK CONSTRAINT RULES:
                - Be EXTREMELY concise, direct, and short. 
                - Your entire feedback MUST have a MAXIMUM of 3 to 4 short bullet points in total.
                - Structure the feedback text exactly like this template:
                
                  ⚠️ Gaps:
                  • [Short bullet point]
                
                  💪 Strengths:
                  • [Short bullet point]
                
                  🚀 Improvements:
                  • [Short bullet point]
                
                OUTPUT RULES:
                - Respond in the SAME LANGUAGE used in the Resume.
                - Do NOT write introductory paragraphs.
                
                RESUME:
                {resumeText}
                
                JOB DESCRIPTION:
                {jobDescription}
                
                {format}
                """;

        AiAnalysisResult result = chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(promptTemplate)
                        .param("currentYear", currentYear)
                        .param("resumeText", resumeText)
                        .param("jobDescription", jobDescription)
                        .param("format", outputConverter.getFormat())
                )
                .call()
                .entity(outputConverter);

        // 2. O Java assume o cálculo da média ponderada de forma instantânea e 100% precisa
        int calculatedOverallScore = (int) Math.round(
                (result.hardSkillsScore() * 0.35) +
                        (result.experienceScore() * 0.35) +
                        (result.softSkillsScore() * 0.20) +
                        (result.educationScore() * 0.10)
        );

        // 3. Retorna o record montado com o Score Geral exato
        return new AiAnalysisResult(
                calculatedOverallScore,
                result.hardSkillsScore(),
                result.experienceScore(),
                result.softSkillsScore(),
                result.educationScore(),
                result.feedback()
        );
    }
}
