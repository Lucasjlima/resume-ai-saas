package saas.com.br.resume_ai_saas.resume.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import saas.com.br.resume_ai_saas.exception.ErrorResponse;
import saas.com.br.resume_ai_saas.exception.GlobalExceptionHandler;
import saas.com.br.resume_ai_saas.exception.ResumeExceptionHandlerAdvice;
import saas.com.br.resume_ai_saas.resume.entity.ExtractionMethod;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.resume.exception.ResumeNotFoundException;
import saas.com.br.resume_ai_saas.resume.exception.UserNotFoundException;
import saas.com.br.resume_ai_saas.resume.service.ResumeService;
import saas.com.br.resume_ai_saas.user.entity.User;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResumeController.class)
@Import({ResumeExceptionHandlerAdvice.class, GlobalExceptionHandler.class, ErrorResponse.class})
@DisplayName("ResumeController MockMvc Tests")
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResumeService resumeService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Resume testResume;

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
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
    }

    @Test
    @DisplayName("GET /api/users/{userId}/resumes should return 200 with list of resumes")
    void getByUser_shouldReturn200_withListOfResumes() throws Exception {
        when(resumeService.findByUserId(1L)).thenReturn(List.of(testResume));

        mockMvc.perform(get("/api/users/1/resumes"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].fileName").value("resume.pdf"))
                .andExpect(jsonPath("$[0].extractionMethod").value("PDF_PARSER"));
    }

    @Test
    @DisplayName("GET /api/users/{userId}/resumes should return 200 with empty list when no resumes")
    void getByUser_shouldReturn200_withEmptyList_whenNoResumes() throws Exception {
        when(resumeService.findByUserId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/users/1/resumes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/users/{userId}/resumes/{resumeId} should return 200 with resume")
    void getById_shouldReturn200_withResume() throws Exception {
        when(resumeService.findById(10L)).thenReturn(testResume);

        mockMvc.perform(get("/api/users/1/resumes/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.fileName").value("resume.pdf"))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.fileUrl").value("local://uploads/1/resume.pdf"));
    }

    @Test
    @DisplayName("GET /api/users/{userId}/resumes/{resumeId} should return 404 when resume not found")
    void getById_shouldReturn404_whenResumeNotFound() throws Exception {
        when(resumeService.findById(99L)).thenThrow(new ResumeNotFoundException(99L));

        mockMvc.perform(get("/api/users/1/resumes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("99")));
    }

    @Test
    @DisplayName("POST /api/users/{userId}/resumes should return 201 when valid PDF uploaded")
    void upload_shouldReturn201_whenValidPdfUploaded() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "PDF content".getBytes()
        );
        when(resumeService.upload(eq(1L), any())).thenReturn(testResume);

        mockMvc.perform(multipart("/api/users/1/resumes").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.fileName").value("resume.pdf"))
                .andExpect(jsonPath("$.extractionMethod").value("PDF_PARSER"));
    }

    @Test
    @DisplayName("POST /api/users/{userId}/resumes should return 404 when user not found")
    void upload_shouldReturn404_whenUserNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "PDF content".getBytes()
        );
        when(resumeService.upload(eq(99L), any()))
                .thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(multipart("/api/users/99/resumes").file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("99")));
    }

    @Test
    @DisplayName("POST /api/users/{userId}/resumes should return 201 with correct userId in response")
    void upload_shouldReturn201_withCorrectUserId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "content".getBytes()
        );
        when(resumeService.upload(eq(1L), any())).thenReturn(testResume);

        mockMvc.perform(multipart("/api/users/1/resumes").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @DisplayName("DELETE /api/users/{userId}/resumes/{resumeId} should return 204")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(resumeService).delete(10L);

        mockMvc.perform(delete("/api/users/1/resumes/10"))
                .andExpect(status().isNoContent());

        verify(resumeService).delete(10L);
    }

    @Test
    @DisplayName("DELETE /api/users/{userId}/resumes/{resumeId} should return 404 when resume not found")
    void delete_shouldReturn404_whenResumeNotFound() throws Exception {
        doThrow(new ResumeNotFoundException(99L)).when(resumeService).delete(99L);

        mockMvc.perform(delete("/api/users/1/resumes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
