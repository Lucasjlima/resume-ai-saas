package saas.com.br.resume_ai_saas.analyse.mapper;

import saas.com.br.resume_ai_saas.analyse.dto.response.AiAnalysisResult;
import saas.com.br.resume_ai_saas.analyse.dto.response.AnalysisResponse;
import saas.com.br.resume_ai_saas.analyse.dto.response.Feedback;
import saas.com.br.resume_ai_saas.analyse.entity.Analysis;
import saas.com.br.resume_ai_saas.analyse.entity.AnalysisStatus;
import saas.com.br.resume_ai_saas.resume.entity.Resume;

import java.time.Instant;
import java.util.List;

public final class AnalysisMapper {

    private AnalysisMapper() {}

    public static AnalysisResponse toResponse(Analysis analysis) {
        return new AnalysisResponse(
                analysis.getId(),
                analysis.getResume().getId(),
                analysis.getOverallScore(),
                analysis.getFeedback(),
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
                .feedback(new Feedback(List.of(), List.of(), List.of()))
                .createdAt(Instant.now())
                .build();
    }

    public static void applySuccess(Analysis analysis, AiAnalysisResult result) {
        analysis.setOverallScore(result.overallScore());
        analysis.setFeedback(result.feedback());
        analysis.setStatus(AnalysisStatus.COMPLETED);
    }

    public static void applyFailure(Analysis analysis, Exception error) {
        analysis.setStatus(AnalysisStatus.FAILED);
        analysis.setFeedback(new Feedback(List.of(), List.of(), List.of(error.getMessage())));
    }
}
