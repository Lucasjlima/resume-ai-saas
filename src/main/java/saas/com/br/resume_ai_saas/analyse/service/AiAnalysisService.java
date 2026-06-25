package saas.com.br.resume_ai_saas.analyse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiAnalysisService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiAnalysisService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public AiAnalysisResult analyze(String resumeText, String jobDescription) {
        String prompt = buildPrompt(resumeText, jobDescription);
        String raw = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        return parseResponse(raw);
    }

    private String buildPrompt(String resumeText, String jobDescription) {
        return """
                You are an expert resume evaluator. Analyze the following resume against the job description.

                RESUME:
                %s

                JOB DESCRIPTION:
                %s

                Respond ONLY with valid JSON in this exact format (no markdown, no extra text):
                {
                  "overallScore": <integer 0-100>,
                  "feedback": {
                    "strengths": [<list of strength strings>],
                    "gaps": [<list of gap strings>],
                    "improvements": [<list of actionable improvement strings>]
                  }
                }
                """.formatted(resumeText, jobDescription);
    }

    private AiAnalysisResult parseResponse(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start == -1 || end == -1) {
            throw new RuntimeException("AI returned invalid JSON response");
        }
        String json = cleaned.substring(start, end + 1);
        try {
            JsonNode root = objectMapper.readTree(json);
            int overallScore = root.path("overallScore").asInt();
            JsonNode feedback = root.path("feedback");
            String feedbackJson = "{\"feedback\":" + objectMapper.writeValueAsString(feedback) + "}";
            return new AiAnalysisResult(overallScore, feedbackJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }

    public record AiAnalysisResult(int overallScore, String feedbackJson) {}
}
