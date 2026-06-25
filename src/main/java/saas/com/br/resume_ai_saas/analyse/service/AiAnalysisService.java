package saas.com.br.resume_ai_saas.analyse.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiAnalysisService {

    private final ChatClient chatClient;

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

        int scoreStart = json.indexOf("\"overallScore\"") + 15;
        int scoreEnd = json.indexOf(',', scoreStart);
        if (scoreEnd == -1) scoreEnd = json.indexOf('}', scoreStart);
        int overallScore = Integer.parseInt(json.substring(scoreStart, scoreEnd).trim());

        int feedbackStart = json.indexOf("\"feedback\"");
        int feedbackBraceOpen = json.indexOf('{', feedbackStart);
        int feedbackBraceClose = findMatchingBrace(json, feedbackBraceOpen);
        String feedbackJson = "{\"feedback\":" + json.substring(feedbackBraceOpen, feedbackBraceClose + 1) + "}";

        return new AiAnalysisResult(overallScore, feedbackJson);
    }

    private int findMatchingBrace(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            if (s.charAt(i) == '{') depth++;
            else if (s.charAt(i) == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return s.length() - 1;
    }

    public record AiAnalysisResult(int overallScore, String feedbackJson) {}
}
