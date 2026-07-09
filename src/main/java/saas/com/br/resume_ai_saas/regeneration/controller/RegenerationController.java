package saas.com.br.resume_ai_saas.regeneration.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import saas.com.br.resume_ai_saas.regeneration.dto.response.RegenerationResponse;
import saas.com.br.resume_ai_saas.regeneration.mapper.RegenerationMapper;
import saas.com.br.resume_ai_saas.regeneration.service.RegenerationService;
import saas.com.br.resume_ai_saas.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes/{resumeId}")
@RequiredArgsConstructor
public class RegenerationController {

    private final RegenerationService regenerationService;

    @PostMapping("/analyses/{analysisId}/regenerations")
    @PreAuthorize("@resumeSecurity.isOwner(#resumeId)")
    public ResponseEntity<RegenerationResponse> trigger(@PathVariable UUID resumeId,
                                                        @PathVariable Long analysisId) {
        UUID userId = AuthenticatedUser.getId();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(RegenerationMapper.toResponse(
                        regenerationService.regenerate(resumeId, analysisId, userId)));
    }

    @GetMapping("/analyses/{analysisId}/regenerations")
    @PreAuthorize("@resumeSecurity.isOwner(#resumeId)")
    public ResponseEntity<List<RegenerationResponse>> listByAnalysis(@PathVariable UUID resumeId,
                                                                     @PathVariable Long analysisId) {
        List<RegenerationResponse> responses = regenerationService.findByAnalysis(resumeId, analysisId).stream()
                .map(RegenerationMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/regenerations/{regenerationId}")
    @PreAuthorize("@resumeSecurity.isOwner(#resumeId)")
    public ResponseEntity<RegenerationResponse> findById(@PathVariable UUID resumeId,
                                                         @PathVariable UUID regenerationId) {
        return ResponseEntity.ok(RegenerationMapper.toResponse(
                regenerationService.findByIdForResume(regenerationId, resumeId)));
    }

    @GetMapping("/regenerations/{regenerationId}/download")
    @PreAuthorize("@resumeSecurity.isOwner(#resumeId)")
    public ResponseEntity<byte[]> download(@PathVariable UUID resumeId,
                                           @PathVariable UUID regenerationId) {
        byte[] pdf = regenerationService.downloadPdf(regenerationId, resumeId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + regenerationId + ".pdf\"")
                .body(pdf);
    }
}
