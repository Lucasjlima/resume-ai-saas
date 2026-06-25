package saas.com.br.resume_ai_saas.analyse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import saas.com.br.resume_ai_saas.analyse.dto.AnalysisRequest;
import saas.com.br.resume_ai_saas.analyse.entity.Analysis;
import saas.com.br.resume_ai_saas.analyse.entity.AnalysisStatus;
import saas.com.br.resume_ai_saas.analyse.exception.AnalysisNotFoundException;
import saas.com.br.resume_ai_saas.analyse.service.AnalysisService;
import saas.com.br.resume_ai_saas.exception.AnalysisExceptionHandlerAdvice;
import saas.com.br.resume_ai_saas.exception.ErrorResponse;
import saas.com.br.resume_ai_saas.exception.GlobalExceptionHandler;
import saas.com.br.resume_ai_saas.resume.entity.ExtractionMethod;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.user.entity.User;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalysisController.class)
@Import({AnalysisExceptionHandlerAdvice.class, GlobalExceptionHandler.class, ErrorResponse.class})
@DisplayName("AnalysisController MockMvc Tests")
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalysisService analysisService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Resume testResume;
    private Analysis testAnalysis;

    // A valid job description that satisfies the @Size(min=50) constraint
    private static final String VALID_JOB_DESCRIPTION =
            "We are looking for a senior Java developer with 5+ years of experience in Spring Boot and microservices architecture.";

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
                .rawText("Sample resume text")
                .extractionMethod(ExtractionMethod.PDF_PARSER)
                .createdAt(Instant.now())
                .build();

        testAnalysis = Analysis.builder()
                .id(100L)
                .resume(testResume)
                .overallScore(85)
                .feedbackJson("{\"feedback\":{\"strengths\":[\"Java skills\"]}}")
                .jobDescription(VALID_JOB_DESCRIPTION)
                .status(AnalysisStatus.COMPLETED)
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
    }

    @Test
    @DisplayName("GET /api/resumes/{resumeId}/analyses should return 200 with list of analyses")
    void getByResume_shouldReturn200_withListOfAnalyses() throws Exception {
        when(analysisService.findByResumeId(10L)).thenReturn(List.of(testAnalysis));

        mockMvc.perform(get("/api/resumes/10/analyses"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(100L))
                .andExpect(jsonPath("$[0].overallScore").value(85))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].resumeId").value(10L));
    }

    @Test
    @DisplayName("GET /api/resumes/{resumeId}/analyses should return 200 with empty list when no analyses")
    void getByResume_shouldReturn200_withEmptyList() throws Exception {
        when(analysisService.findByResumeId(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/resumes/10/analyses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/resumes/{resumeId}/analyses/{analysisId} should return 200 with analysis")
    void getById_shouldReturn200_withAnalysis() throws Exception {
        when(analysisService.findById(100L)).thenReturn(testAnalysis);

        mockMvc.perform(get("/api/resumes/10/analyses/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.overallScore").value(85))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.feedbackJson").value(containsString("strengths")));
    }

    @Test
    @DisplayName("GET /api/resumes/{resumeId}/analyses/{analysisId} should return 404 when not found")
    void getById_shouldReturn404_whenNotFound() throws Exception {
        when(analysisService.findById(99L)).thenThrow(new AnalysisNotFoundException(99L));

        mockMvc.perform(get("/api/resumes/10/analyses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("99")));
    }

    @Test
    @DisplayName("POST /api/resumes/{resumeId}/analyses should return 400 when jobDescription is blank")
    void analyze_shouldReturn400_whenJobDescriptionIsBlank() throws Exception {
        AnalysisRequest request = new AnalysisRequest("");

        mockMvc.perform(post("/api/resumes/10/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(analysisService, never()).analyze(any(), anyString());
    }

    @Test
    @DisplayName("POST /api/resumes/{resumeId}/analyses should return 400 when jobDescription is too short (less than 50 chars)")
    void analyze_shouldReturn400_whenJobDescriptionTooShort() throws Exception {
        // Less than 50 characters
        AnalysisRequest request = new AnalysisRequest("Too short description.");

        mockMvc.perform(post("/api/resumes/10/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(analysisService, never()).analyze(any(), anyString());
    }

    @Test
    @DisplayName("POST /api/resumes/{resumeId}/analyses should return 201 with analysis when valid body")
    void analyze_shouldReturn201_whenValidBody() throws Exception {
        AnalysisRequest request = new AnalysisRequest(VALID_JOB_DESCRIPTION);
        when(analysisService.analyze(eq(10L), eq(VALID_JOB_DESCRIPTION))).thenReturn(testAnalysis);

        mockMvc.perform(post("/api/resumes/10/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.overallScore").value(85))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.resumeId").value(10L));
    }

    @Test
    @DisplayName("POST /api/resumes/{resumeId}/analyses should return 201 with correct jobDescription in response")
    void analyze_shouldReturn201_withJobDescriptionInResponse() throws Exception {
        AnalysisRequest request = new AnalysisRequest(VALID_JOB_DESCRIPTION);
        when(analysisService.analyze(eq(10L), anyString())).thenReturn(testAnalysis);

        mockMvc.perform(post("/api/resumes/10/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobDescription").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/resumes/{resumeId}/analyses should return 400 when request body is null")
    void analyze_shouldReturn400_whenRequestBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/resumes/10/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
