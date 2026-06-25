package saas.com.br.resume_ai_saas.analyse.mapper;

import saas.com.br.resume_ai_saas.analyse.dto.response.AnalysisResponse;
import saas.com.br.resume_ai_saas.analyse.entity.Analysis;

public final class AnalysisMapper {

    private AnalysisMapper() {}

    public static AnalysisResponse toResponse(Analysis analysis) {
        return new AnalysisResponse(
                analysis.getId(),
                analysis.getResume().getId(),
                analysis.getOverallScore(),
                analysis.getFeedbackJson(),
                analysis.getJobDescription(),
                analysis.getStatus(),
                analysis.getCreatedAt()
        );
    }
}
