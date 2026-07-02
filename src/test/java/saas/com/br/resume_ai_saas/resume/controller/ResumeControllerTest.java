package saas.com.br.resume_ai_saas.resume.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import saas.com.br.resume_ai_saas.exception.GlobalExceptionHandler;
import saas.com.br.resume_ai_saas.exception.ResumeNotFoundException;
import saas.com.br.resume_ai_saas.resume.entity.ExtractionMethod;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.resume.entity.ResumeStatus;
import saas.com.br.resume_ai_saas.resume.service.ResumeService;
import saas.com.br.resume_ai_saas.security.ResumeSecurity;
import saas.com.br.resume_ai_saas.security.SecurityConfig;
import saas.com.br.resume_ai_saas.security.SecurityEntryPoints;
import saas.com.br.resume_ai_saas.security.SupabaseJwtFilter;
import saas.com.br.resume_ai_saas.security.SupabaseUserDetails;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HTTP-level IDOR checks. Method security is enabled so the {@code @PreAuthorize}
 * gate on delete is exercised; the SecurityContext is populated manually (filters
 * are disabled to avoid the network-backed JWT/JWKS filter).
 */
@WebMvcTest(controllers = ResumeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SupabaseJwtFilter.class, SecurityConfig.class, SecurityEntryPoints.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class,
        ResumeControllerTest.MethodSecurityConfig.class})
@DisplayName("ResumeController IDOR / authorization")
class ResumeControllerTest {

    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResumeService resumeService;

    @MockitoBean(name = "resumeSecurity")
    private ResumeSecurity resumeSecurity;

    private final UUID userId = UUID.randomUUID();
    private final UUID resumeId = UUID.randomUUID();

    @BeforeEach
    void authenticate() {
        var principal = new SupabaseUserDetails(userId.toString(), "user@example.com", "authenticated", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /{id}: owner receives 200")
    void getById_owner_returns200() throws Exception {
        when(resumeService.findByIdForUser(resumeId, userId)).thenReturn(sampleResume());

        mockMvc.perform(get("/api/resumes/{id}", resumeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resumeId.toString()))
                .andExpect(jsonPath("$.fileName").value("resume.pdf"));
    }

    @Test
    @DisplayName("GET /{id}: another user's resume resolves to 404 (no existence leak)")
    void getById_crossUser_returns404() throws Exception {
        when(resumeService.findByIdForUser(eq(resumeId), eq(userId)))
                .thenThrow(new ResumeNotFoundException(resumeId));

        mockMvc.perform(get("/api/resumes/{id}", resumeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /{id}: owner soft-delete returns 204")
    void delete_owner_returns204() throws Exception {
        when(resumeSecurity.isOwner(resumeId)).thenReturn(true);

        mockMvc.perform(delete("/api/resumes/{id}", resumeId))
                .andExpect(status().isNoContent());

        verify(resumeService).softDelete(resumeId, userId);
    }

    @Test
    @DisplayName("DELETE /{id}: non-owner is rejected with 403 before the service is called")
    void delete_nonOwner_returns403_andNeverCallsService() throws Exception {
        when(resumeSecurity.isOwner(resumeId)).thenReturn(false);

        mockMvc.perform(delete("/api/resumes/{id}", resumeId))
                .andExpect(status().isForbidden());

        verify(resumeService, never()).softDelete(any(), any());
    }

    private Resume sampleResume() {
        return Resume.builder()
                .id(resumeId)
                .userId(userId)
                .fileName("resume.pdf")
                .rawText("full text")
                .status(ResumeStatus.COMPLETED)
                .extractionMethod(ExtractionMethod.PDF_PARSER)
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
    }
}
