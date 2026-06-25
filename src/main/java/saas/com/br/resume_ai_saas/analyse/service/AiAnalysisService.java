package saas.com.br.resume_ai_saas.analyse.service;

import com.google.gson.Gson;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiAnalysisService {

    private final ChatClient chatClient;
    private final Gson gson = new Gson();

    public AiAnalysisService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public AiAnalysisResult analyze(String resumeText, String jobDescription) {
        String raw = chatClient.prompt()
                .user(buildPrompt(resumeText, jobDescription))
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
        String json = raw.trim().replaceAll("(?s)```json\\s*|```", "").trim();
        return gson.fromJson(json, AiAnalysisResult.class);
    }

    public record Feedback(List<String> strengths, List<String> gaps, List<String> improvements) {}
    public record AiAnalysisResult(int overallScore, Feedback feedback) {}
}
