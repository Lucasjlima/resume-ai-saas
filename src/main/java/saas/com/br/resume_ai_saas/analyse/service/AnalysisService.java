package saas.com.br.resume_ai_saas.analyse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.com.br.resume_ai_saas.analyse.entity.Analysis;
import saas.com.br.resume_ai_saas.analyse.entity.AnalysisStatus;
import saas.com.br.resume_ai_saas.analyse.exception.AnalysisNotFoundException;
import saas.com.br.resume_ai_saas.analyse.repository.AnalysisRepository;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.resume.repository.ResumeRepository;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final ResumeRepository resumeRepository;
    private final AiAnalysisService aiAnalysisService;

    @Transactional(readOnly = true)
    public List<Analysis> findByResumeId(Long resumeId) {
        return analysisRepository.findByResumeId(resumeId);
    }

    @Transactional(readOnly = true)
    public Analysis findById(Long id) {
        return analysisRepository.findById(id)
                .orElseThrow(() -> new AnalysisNotFoundException(id));
    }

    @Transactional
    public Analysis analyze(Long resumeId, String jobDescription) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found with id: " + resumeId));

        Analysis analysis = Analysis.builder()
                .resume(resume)
                .jobDescription(jobDescription)
                .status(AnalysisStatus.PENDING)
                .overallScore(0)
                .feedbackJson("{}")
                .createdAt(Instant.now())
                .build();
        analysis = analysisRepository.save(analysis);

        try {
            AiAnalysisService.AiAnalysisResult result =
                    aiAnalysisService.analyze(resume.getRawText(), jobDescription);
            analysis.setOverallScore(result.overallScore());
            analysis.setFeedbackJson(result.feedbackJson());
            analysis.setStatus(AnalysisStatus.COMPLETED);
        } catch (Exception e) {
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setFeedbackJson("{\"error\":\"" + e.getMessage() + "\"}");
        }

        return analysisRepository.save(analysis);
    }
}
