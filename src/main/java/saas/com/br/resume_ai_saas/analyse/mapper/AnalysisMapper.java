package saas.com.br.resume_ai_saas.analyse.mapper;

import com.google.gson.Gson;
import saas.com.br.resume_ai_saas.analyse.dto.response.AiAnalysisResult;
import saas.com.br.resume_ai_saas.analyse.dto.response.AnalysisResponse;
import saas.com.br.resume_ai_saas.analyse.entity.Analysis;
import saas.com.br.resume_ai_saas.analyse.entity.AnalysisStatus;
import saas.com.br.resume_ai_saas.resume.entity.Resume;

import java.time.Instant;

public final class AnalysisMapper {

    private static final Gson GSON = new Gson();

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

    public static Analysis toPendingAnalysis(Resume resume, String jobDescription) {
        return Analysis.builder()
                .resume(resume)
                .jobDescription(jobDescription)
                .status(AnalysisStatus.PENDING)
                .overallScore(0)
                .feedbackJson("{}")
                .createdAt(Instant.now())
                .build();
    }

    public static void applySuccess(Analysis analysis, AiAnalysisResult result) {
        analysis.setOverallScore(result.overallScore());
        analysis.setFeedbackJson(GSON.toJson(result.feedback()));
        analysis.setStatus(AnalysisStatus.COMPLETED);
    }

    public static void applyFailure(Analysis analysis, Exception error) {
        analysis.setStatus(AnalysisStatus.FAILED);
        analysis.setFeedbackJson("{\"error\":\"" + error.getMessage() + "\"}");
    }
}
