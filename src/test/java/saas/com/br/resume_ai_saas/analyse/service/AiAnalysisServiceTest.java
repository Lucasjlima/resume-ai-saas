package saas.com.br.resume_ai_saas.analyse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiAnalysisService Unit Tests")
class AiAnalysisServiceTest {

    @Mock
    private ChatClient.Builder mockBuilder;

    @Mock
    private ChatClient mockChatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec mockSpec;

    @Mock
    private ChatClient.CallResponseSpec mockCallSpec;

    private AiAnalysisService aiAnalysisService;

    private static final String VALID_JSON_RESPONSE = """
            {
              "overallScore": 85,
              "feedback": {
                "strengths": ["Strong technical skills", "Good communication"],
                "gaps": ["Missing cloud experience"],
                "improvements": ["Add AWS certifications", "Include more metrics"]
              }
            }
            """;

    private static final String MARKDOWN_WRAPPED_JSON = """
            ```json
            {
              "overallScore": 72,
              "feedback": {
                "strengths": ["Java expertise"],
                "gaps": ["Leadership experience"],
                "improvements": ["Highlight leadership roles"]
              }
            }
            ```
            """;

    @BeforeEach
    void setUp() {
        when(mockBuilder.build()).thenReturn(mockChatClient);
        when(mockChatClient.prompt()).thenReturn(mockSpec);
        when(mockSpec.user(anyString())).thenReturn(mockSpec);
        when(mockSpec.call()).thenReturn(mockCallSpec);

        aiAnalysisService = new AiAnalysisService(mockBuilder);
    }

    @Test
    @DisplayName("analyze() should return AiAnalysisResult with correct score from valid JSON")
    void analyze_shouldReturnResult_withValidJson() {
        when(mockCallSpec.content()).thenReturn(VALID_JSON_RESPONSE);

        AiAnalysisService.AiAnalysisResult result = aiAnalysisService.analyze(
                "Software engineer with 5 years experience",
                "Looking for a senior Java developer"
        );

        assertThat(result).isNotNull();
        assertThat(result.overallScore()).isEqualTo(85);
        assertThat(result.feedbackJson()).isNotNull();
        assertThat(result.feedbackJson()).contains("feedback");
    }

    @Test
    @DisplayName("analyze() should parse correctly when score is within 0-100 range")
    void analyze_shouldReturnScoreWithinValidRange() {
        when(mockCallSpec.content()).thenReturn(VALID_JSON_RESPONSE);

        AiAnalysisService.AiAnalysisResult result = aiAnalysisService.analyze(
                "resume text", "job description"
        );

        assertThat(result.overallScore()).isBetween(0, 100);
    }

    @Test
    @DisplayName("analyze() should parse markdown-wrapped JSON (```json ... ```) correctly")
    void analyze_shouldParseMarkdownWrappedJson() {
        when(mockCallSpec.content()).thenReturn(MARKDOWN_WRAPPED_JSON);

        AiAnalysisService.AiAnalysisResult result = aiAnalysisService.analyze(
                "resume text", "job description"
        );

        assertThat(result).isNotNull();
        assertThat(result.overallScore()).isEqualTo(72);
        assertThat(result.feedbackJson()).contains("strengths");
    }

    @Test
    @DisplayName("analyze() should throw RuntimeException when AI returns invalid JSON (no braces)")
    void analyze_shouldThrowRuntimeException_whenInvalidJson() {
        when(mockCallSpec.content()).thenReturn("This is not valid JSON at all");

        assertThatThrownBy(() -> aiAnalysisService.analyze("resume text", "job description"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("invalid JSON");
    }

    @Test
    @DisplayName("overallScore should be extracted correctly from response JSON")
    void analyze_shouldExtractOverallScoreCorrectly() {
        String jsonWith42Score = """
                {
                  "overallScore": 42,
                  "feedback": {
                    "strengths": ["skill1"],
                    "gaps": [],
                    "improvements": []
                  }
                }
                """;
        when(mockCallSpec.content()).thenReturn(jsonWith42Score);

        AiAnalysisService.AiAnalysisResult result = aiAnalysisService.analyze(
                "resume text", "job description"
        );

        assertThat(result.overallScore()).isEqualTo(42);
    }

    @Test
    @DisplayName("feedbackJson should contain the nested feedback object")
    void analyze_shouldReturnFeedbackJsonWithFeedbackObject() {
        when(mockCallSpec.content()).thenReturn(VALID_JSON_RESPONSE);

        AiAnalysisService.AiAnalysisResult result = aiAnalysisService.analyze(
                "resume text", "job description"
        );

        assertThat(result.feedbackJson()).contains("\"feedback\"");
        assertThat(result.feedbackJson()).contains("strengths");
        assertThat(result.feedbackJson()).contains("gaps");
        assertThat(result.feedbackJson()).contains("improvements");
    }

    @Test
    @DisplayName("analyze() should pass the prompt containing both resume and job description to ChatClient")
    void analyze_shouldPassPromptToChatClient() {
        when(mockCallSpec.content()).thenReturn(VALID_JSON_RESPONSE);

        aiAnalysisService.analyze("MY RESUME CONTENT", "MY JOB DESCRIPTION");

        verify(mockSpec).user(argThat(prompt ->
                prompt.contains("MY RESUME CONTENT") && prompt.contains("MY JOB DESCRIPTION")
        ));
    }

    @Test
    @DisplayName("analyze() with score 0 should parse correctly")
    void analyze_shouldHandleScoreZero() {
        String zeroScoreJson = """
                {
                  "overallScore": 0,
                  "feedback": {
                    "strengths": [],
                    "gaps": ["No relevant skills"],
                    "improvements": ["Start from scratch"]
                  }
                }
                """;
        when(mockCallSpec.content()).thenReturn(zeroScoreJson);

        AiAnalysisService.AiAnalysisResult result = aiAnalysisService.analyze(
                "resume text", "job description"
        );

        assertThat(result.overallScore()).isEqualTo(0);
    }

    @Test
    @DisplayName("analyze() with score 100 should parse correctly")
    void analyze_shouldHandleScoreHundred() {
        String perfectScoreJson = """
                {
                  "overallScore": 100,
                  "feedback": {
                    "strengths": ["Perfect match"],
                    "gaps": [],
                    "improvements": []
                  }
                }
                """;
        when(mockCallSpec.content()).thenReturn(perfectScoreJson);

        AiAnalysisService.AiAnalysisResult result = aiAnalysisService.analyze(
                "resume text", "job description"
        );

        assertThat(result.overallScore()).isEqualTo(100);
    }
}
