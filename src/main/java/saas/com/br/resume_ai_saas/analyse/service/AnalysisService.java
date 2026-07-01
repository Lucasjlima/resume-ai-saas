package saas.com.br.resume_ai_saas.analyse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import saas.com.br.resume_ai_saas.analyse.dto.response.AiAnalysisResult;
import saas.com.br.resume_ai_saas.analyse.entity.Analysis;
import saas.com.br.resume_ai_saas.analyse.exception.AnalysisNotFoundException;
import saas.com.br.resume_ai_saas.analyse.mapper.AnalysisMapper;
import saas.com.br.resume_ai_saas.analyse.repository.AnalysisRepository;
import saas.com.br.resume_ai_saas.exception.ResumeNotFoundException;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.resume.repository.ResumeRepository;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final ResumeRepository resumeRepository;
    private final AiAnalysisService aiAnalysisService;
    private final AnalysisService self;

    public AnalysisService(AnalysisRepository analysisRepository,
                           ResumeRepository resumeRepository,
                           AiAnalysisService aiAnalysisService,
                           @Lazy AnalysisService self) {
        this.analysisRepository = analysisRepository;
        this.resumeRepository = resumeRepository;
        this.aiAnalysisService = aiAnalysisService;
        this.self = self;
    }

    @Transactional(readOnly = true)
    public List<Analysis> findByResumeId(UUID resumeId) {
        return analysisRepository.findByResumeId(resumeId);
    }

    @Transactional(readOnly = true)
    public Analysis findById(Long id) {
        return analysisRepository.findById(id)
                .orElseThrow(() -> new AnalysisNotFoundException(id));
    }

    public Analysis analyze(UUID resumeId, String jobDescription) {
        Analysis analysis = self.initializeAnalysis(resumeId, jobDescription);
        self.runAiAnalysisAsync(analysis);
        return analysis;
    }

    @Async
    public void runAiAnalysisAsync(Analysis analysis) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        log.info("[Análise {}] Iniciando processamento assíncrono da IA...", analysis.getId());
        try {
            AiAnalysisResult result =
                    aiAnalysisService.analyze(analysis.getResume().getRawText(), analysis.getJobDescription());
            self.completeAnalysisWithSuccess(analysis.getId(), result);
            stopWatch.stop();
            log.info("[Análise {}] Finalizada com SUCESSO em {} segundos ({} ms)",
                    analysis.getId(), stopWatch.getTotalTimeSeconds(), stopWatch.getTotalTimeMillis());
        } catch (Exception e) {
            self.completeAnalysisWithFailure(analysis.getId(), e);
        }
    }

    @Transactional
    public Analysis initializeAnalysis(UUID resumeId, String jobDescription) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResumeNotFoundException(resumeId));

        Analysis analysis = AnalysisMapper.toPendingAnalysis(resume, jobDescription);
        return analysisRepository.save(analysis);
    }

    @Transactional
    public void completeAnalysisWithSuccess(Long analysisId, AiAnalysisResult result) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));
        AnalysisMapper.applySuccess(analysis, result);
    }

    @Transactional
    public void completeAnalysisWithFailure(Long analysisId, Exception error) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));
        AnalysisMapper.applyFailure(analysis, error);
    }
}
