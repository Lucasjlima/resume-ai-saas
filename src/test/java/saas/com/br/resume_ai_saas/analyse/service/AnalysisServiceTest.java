package saas.com.br.resume_ai_saas.analyse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saas.com.br.resume_ai_saas.analyse.entity.Analysis;
import saas.com.br.resume_ai_saas.analyse.entity.AnalysisStatus;
import saas.com.br.resume_ai_saas.analyse.exception.AnalysisNotFoundException;
import saas.com.br.resume_ai_saas.analyse.repository.AnalysisRepository;
import saas.com.br.resume_ai_saas.resume.entity.ExtractionMethod;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.resume.repository.ResumeRepository;
import saas.com.br.resume_ai_saas.user.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalysisService Unit Tests")
class AnalysisServiceTest {

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private AiAnalysisService aiAnalysisService;

    @InjectMocks
    private AnalysisService analysisService;

    private User testUser;
    private Resume testResume;
    private Analysis testAnalysis;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .createdAt(Instant.now())
                .build();

        testResume = Resume.builder()
                .id(10L)
                .user(testUser)
                .fileName("resume.pdf")
                .fileUrl("local://uploads/1/resume.pdf")
                .rawText("Experienced Java developer with 5 years experience")
                .extractionMethod(ExtractionMethod.PDF_PARSER)
                .createdAt(Instant.now())
                .build();

        testAnalysis = Analysis.builder()
                .id(100L)
                .resume(testResume)
                .overallScore(85)
                .feedbackJson("{\"feedback\":{\"strengths\":[\"Java skills\"]}}")
                .jobDescription("Looking for a senior Java developer")
                .status(AnalysisStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("analyze() should save analysis with PENDING status before calling AI")
    void analyze_shouldSavePendingStatusBeforeCallingAi() {
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(testResume));
        when(aiAnalysisService.analyze(anyString(), anyString()))
                .thenReturn(new AiAnalysisService.AiAnalysisResult(85,
                        "{\"feedback\":{\"strengths\":[\"Java\"]}}"));

        ArgumentCaptor<Analysis> captor = ArgumentCaptor.forClass(Analysis.class);
        when(analysisRepository.save(captor.capture())).thenAnswer(inv -> {
            Analysis saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        analysisService.analyze(10L, "Senior Java developer position");

        // The first save should have PENDING status
        List<Analysis> allSaved = captor.getAllValues();
        assertThat(allSaved).hasSizeGreaterThanOrEqualTo(1);
        assertThat(allSaved.get(0).getStatus()).isEqualTo(AnalysisStatus.PENDING);
    }

    @Test
    @DisplayName("analyze() should save analysis with COMPLETED status after successful AI call")
    void analyze_shouldSaveCompletedStatus_afterSuccessfulAiCall() {
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(testResume));
        AiAnalysisService.AiAnalysisResult aiResult =
                new AiAnalysisService.AiAnalysisResult(85, "{\"feedback\":{\"strengths\":[]}}");
        when(aiAnalysisService.analyze(anyString(), anyString())).thenReturn(aiResult);

        ArgumentCaptor<Analysis> captor = ArgumentCaptor.forClass(Analysis.class);
        when(analysisRepository.save(captor.capture())).thenAnswer(inv -> {
            Analysis a = inv.getArgument(0);
            if (a.getId() == null) a.setId(100L);
            return a;
        });

        Analysis result = analysisService.analyze(10L, "Senior Java developer");

        // Last captured analysis should be COMPLETED
        List<Analysis> allSaved = captor.getAllValues();
        Analysis lastSaved = allSaved.get(allSaved.size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(lastSaved.getOverallScore()).isEqualTo(85);
    }

    @Test
    @DisplayName("analyze() should save analysis with FAILED status when AI throws exception")
    void analyze_shouldSaveFailedStatus_whenAiThrowsException() {
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(testResume));
        when(aiAnalysisService.analyze(anyString(), anyString()))
                .thenThrow(new RuntimeException("AI service unavailable"));

        ArgumentCaptor<Analysis> captor = ArgumentCaptor.forClass(Analysis.class);
        when(analysisRepository.save(captor.capture())).thenAnswer(inv -> {
            Analysis a = inv.getArgument(0);
            if (a.getId() == null) a.setId(100L);
            return a;
        });

        Analysis result = analysisService.analyze(10L, "Some job description");

        List<Analysis> allSaved = captor.getAllValues();
        Analysis lastSaved = allSaved.get(allSaved.size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(lastSaved.getFeedbackJson()).contains("error");
    }

    @Test
    @DisplayName("analyze() should throw RuntimeException when resume not found")
    void analyze_shouldThrowRuntimeException_whenResumeNotFound() {
        when(resumeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analysisService.analyze(99L, "Some job description"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(aiAnalysisService, never()).analyze(anyString(), anyString());
        verify(analysisRepository, never()).save(any());
    }

    @Test
    @DisplayName("analyze() should pass resume's rawText to AI service")
    void analyze_shouldPassRawTextToAiService() {
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(testResume));
        AiAnalysisService.AiAnalysisResult aiResult =
                new AiAnalysisService.AiAnalysisResult(75, "{\"feedback\":{}}");
        when(aiAnalysisService.analyze(anyString(), anyString())).thenReturn(aiResult);
        when(analysisRepository.save(any())).thenAnswer(inv -> {
            Analysis a = inv.getArgument(0);
            if (a.getId() == null) a.setId(100L);
            return a;
        });

        analysisService.analyze(10L, "Job description here");

        verify(aiAnalysisService).analyze(
                eq("Experienced Java developer with 5 years experience"),
                eq("Job description here")
        );
    }

    @Test
    @DisplayName("findById() should return analysis when found")
    void findById_shouldReturnAnalysis_whenFound() {
        when(analysisRepository.findById(100L)).thenReturn(Optional.of(testAnalysis));

        Analysis result = analysisService.findById(100L);

        assertThat(result).isEqualTo(testAnalysis);
        assertThat(result.getId()).isEqualTo(100L);
        verify(analysisRepository).findById(100L);
    }

    @Test
    @DisplayName("findById() should throw AnalysisNotFoundException when not found")
    void findById_shouldThrowAnalysisNotFoundException_whenNotFound() {
        when(analysisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analysisService.findById(99L))
                .isInstanceOf(AnalysisNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("findByResumeId() should return all analyses for a resume")
    void findByResumeId_shouldReturnAllAnalysesForResume() {
        Analysis anotherAnalysis = Analysis.builder()
                .id(101L)
                .resume(testResume)
                .overallScore(70)
                .feedbackJson("{}")
                .status(AnalysisStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();
        when(analysisRepository.findByResumeId(10L))
                .thenReturn(List.of(testAnalysis, anotherAnalysis));

        List<Analysis> result = analysisService.findByResumeId(10L);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testAnalysis, anotherAnalysis);
        verify(analysisRepository).findByResumeId(10L);
    }

    @Test
    @DisplayName("findByResumeId() should return empty list when resume has no analyses")
    void findByResumeId_shouldReturnEmptyList_whenNoAnalyses() {
        when(analysisRepository.findByResumeId(10L)).thenReturn(List.of());

        List<Analysis> result = analysisService.findByResumeId(10L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("analyze() should set job description on the analysis entity")
    void analyze_shouldSetJobDescriptionOnAnalysis() {
        String jobDescription = "Senior backend developer with Spring Boot";
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(testResume));
        when(aiAnalysisService.analyze(anyString(), anyString()))
                .thenReturn(new AiAnalysisService.AiAnalysisResult(80, "{\"feedback\":{}}"));

        ArgumentCaptor<Analysis> captor = ArgumentCaptor.forClass(Analysis.class);
        when(analysisRepository.save(captor.capture())).thenAnswer(inv -> {
            Analysis a = inv.getArgument(0);
            if (a.getId() == null) a.setId(100L);
            return a;
        });

        analysisService.analyze(10L, jobDescription);

        assertThat(captor.getAllValues().get(0).getJobDescription()).isEqualTo(jobDescription);
    }
}
