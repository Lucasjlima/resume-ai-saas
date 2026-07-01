package saas.com.br.resume_ai_saas.resume.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import saas.com.br.resume_ai_saas.resume.dto.response.ResumeDownloadResponse;
import saas.com.br.resume_ai_saas.resume.dto.response.ResumeResponse;
import saas.com.br.resume_ai_saas.resume.mapper.ResumeMapper;
import saas.com.br.resume_ai_saas.resume.service.ResumeService;
import saas.com.br.resume_ai_saas.security.AuthenticatedUser;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private static final Duration PRESIGN_TTL = Duration.ofMinutes(5);

    private final ResumeService resumeService;

    @GetMapping
    public ResponseEntity<List<ResumeResponse>> findByUser() {
        UUID userId = AuthenticatedUser.getId();
        List<ResumeResponse> responses = resumeService.findByUserId(userId).stream()
                .map(ResumeMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ResumeMapper.toResponse(resumeService.findById(id)));
    }

    @GetMapping("/{id}/download-url")
    public ResponseEntity<ResumeDownloadResponse> getDownloadUrl(@PathVariable UUID id) {
        String url = resumeService.generatePresignedDownloadUrl(id, PRESIGN_TTL);
        return ResponseEntity.ok(new ResumeDownloadResponse(url, PRESIGN_TTL.toSeconds()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponse> upload(@RequestParam("file") MultipartFile file) {
        UUID userId = AuthenticatedUser.getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResumeMapper.toResponse(resumeService.upload(userId, file)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        resumeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
