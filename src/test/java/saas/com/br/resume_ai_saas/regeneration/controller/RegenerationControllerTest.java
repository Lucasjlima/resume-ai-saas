package saas.com.br.resume_ai_saas.regeneration.controller;

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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import saas.com.br.resume_ai_saas.analyse.entity.Analysis;
import saas.com.br.resume_ai_saas.exception.GlobalExceptionHandler;
import saas.com.br.resume_ai_saas.regeneration.entity.RegenerationStatus;
import saas.com.br.resume_ai_saas.regeneration.entity.ResumeRegeneration;
import saas.com.br.resume_ai_saas.regeneration.exception.RegenerationNotFoundException;
import saas.com.br.resume_ai_saas.regeneration.exception.RegenerationNotReadyException;
import saas.com.br.resume_ai_saas.regeneration.exception.RegenerationRateLimitExceededException;
import saas.com.br.resume_ai_saas.regeneration.service.RegenerationService;
import saas.com.br.resume_ai_saas.security.ResumeSecurity;
import saas.com.br.resume_ai_saas.security.SecurityConfig;
import saas.com.br.resume_ai_saas.security.SecurityEntryPoints;
import saas.com.br.resume_ai_saas.security.SupabaseJwtFilter;
import saas.com.br.resume_ai_saas.security.SupabaseUserDetails;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HTTP-level IDOR / flow checks for the regeneration endpoints. Same scaffold
 * as {@code ResumeControllerTest}: method security on, JWT filter off,
 * SecurityContext populated manually.
 */
@WebMvcTest(controllers = RegenerationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SupabaseJwtFilter.class, SecurityConfig.class, SecurityEntryPoints.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class,
        RegenerationControllerTest.MethodSecurityConfig.class})
@DisplayName("RegenerationController IDOR / flow")
class RegenerationControllerTest {

    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegenerationService regenerationService;

    @MockitoBean(name = "resumeSecurity")
    private ResumeSecurity resumeSecurity;

    private final UUID userId = UUID.randomUUID();
    private final UUID resumeId = UUID.randomUUID();
    private final Long analysisId = 42L;
    private final UUID regenerationId = UUID.randomUUID();

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

    private ResumeRegeneration sampleRegeneration(RegenerationStatus status) {
        return ResumeRegeneration.builder()
                .id(regenerationId)
                .analysis(Analysis.builder().id(analysisId).build())
                .status(status)
                .attemptNumber(1)
                .retryCount(0)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    @Test
    @DisplayName("POST regenerations: owner receives 202 with the record id for polling")
    void trigger_owner_returns202() throws Exception {
        when(resumeSecurity.isOwner(resumeId)).thenReturn(true);
        when(regenerationService.regenerate(resumeId, analysisId, userId))
                .thenReturn(sampleRegeneration(RegenerationStatus.PENDING));

        mockMvc.perform(post("/api/resumes/{resumeId}/analyses/{analysisId}/regenerations",
                        resumeId, analysisId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(regenerationId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attemptNumber").value(1));
    }

    @Test
    @DisplayName("POST regenerations: non-owner is rejected with 403 before the service is called")
    void trigger_nonOwner_returns403() throws Exception {
        when(resumeSecurity.isOwner(resumeId)).thenReturn(false);

        mockMvc.perform(post("/api/resumes/{resumeId}/analyses/{analysisId}/regenerations",
                        resumeId, analysisId))
                .andExpect(status().isForbidden());

        verify(regenerationService, never()).regenerate(any(), any(), any());
    }

    @Test
    @DisplayName("POST regenerations: rate limit exceeded resolves to 429 with a clear message")
    void trigger_rateLimited_returns429() throws Exception {
        when(resumeSecurity.isOwner(resumeId)).thenReturn(true);
        when(regenerationService.regenerate(resumeId, analysisId, userId))
                .thenThrow(new RegenerationRateLimitExceededException(5));

        mockMvc.perform(post("/api/resumes/{resumeId}/analyses/{analysisId}/regenerations",
                        resumeId, analysisId))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    @DisplayName("GET regeneration: owner polls status with 200")
    void getById_owner_returns200() throws Exception {
        when(resumeSecurity.isOwner(resumeId)).thenReturn(true);
        when(regenerationService.findByIdForResume(regenerationId, resumeId))
                .thenReturn(sampleRegeneration(RegenerationStatus.PROCESSING));

        mockMvc.perform(get("/api/resumes/{resumeId}/regenerations/{id}", resumeId, regenerationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.analysisId").value(analysisId));
    }

    @Test
    @DisplayName("GET regeneration: cross-resume access resolves to 404 (no existence leak)")
    void getById_chainMismatch_returns404() throws Exception {
        when(resumeSecurity.isOwner(resumeId)).thenReturn(true);
        when(regenerationService.findByIdForResume(regenerationId, resumeId))
                .thenThrow(new RegenerationNotFoundException(regenerationId));

        mockMvc.perform(get("/api/resumes/{resumeId}/regenerations/{id}", resumeId, regenerationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET download: serves the PDF when DONE")
    void download_done_returnsPdf() throws Exception {
        when(resumeSecurity.isOwner(resumeId)).thenReturn(true);
        when(regenerationService.downloadPdf(regenerationId, resumeId)).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/resumes/{resumeId}/regenerations/{id}/download",
                        resumeId, regenerationId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"" + regenerationId + ".pdf\""))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    @DisplayName("GET download: not ready resolves to 409")
    void download_notReady_returns409() throws Exception {
        when(resumeSecurity.isOwner(resumeId)).thenReturn(true);
        when(regenerationService.downloadPdf(regenerationId, resumeId))
                .thenThrow(new RegenerationNotReadyException(regenerationId));

        mockMvc.perform(get("/api/resumes/{resumeId}/regenerations/{id}/download",
                        resumeId, regenerationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET list by analysis: owner receives all attempts")
    void listByAnalysis_owner_returns200() throws Exception {
        when(resumeSecurity.isOwner(resumeId)).thenReturn(true);
        when(regenerationService.findByAnalysis(resumeId, analysisId))
                .thenReturn(List.of(sampleRegeneration(RegenerationStatus.FAILED)));

        mockMvc.perform(get("/api/resumes/{resumeId}/analyses/{analysisId}/regenerations",
                        resumeId, analysisId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(regenerationId.toString()))
                .andExpect(jsonPath("$[0].status").value("FAILED"));
    }

    @Test
    @DisplayName("GET list by analysis: non-owner is rejected with 403")
    void listByAnalysis_nonOwner_returns403() throws Exception {
        when(resumeSecurity.isOwner(resumeId)).thenReturn(false);

        mockMvc.perform(get("/api/resumes/{resumeId}/analyses/{analysisId}/regenerations",
                        resumeId, analysisId))
                .andExpect(status().isForbidden());

        verify(regenerationService, never()).findByAnalysis(any(), any());
    }
}
