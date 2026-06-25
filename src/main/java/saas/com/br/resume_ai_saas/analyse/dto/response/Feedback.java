package saas.com.br.resume_ai_saas.analyse.dto.response;

import java.util.List;

public record Feedback(List<String> strengths, List<String> gaps, List<String> improvements) {}
