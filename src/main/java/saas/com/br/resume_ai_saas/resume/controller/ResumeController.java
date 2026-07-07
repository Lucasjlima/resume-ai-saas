package saas.com.br.resume_ai_saas.resume.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import saas.com.br.resume_ai_saas.resume.dto.response.PageResponse;
import saas.com.br.resume_ai_saas.resume.dto.response.ResumeDownloadResponse;
import saas.com.br.resume_ai_saas.resume.dto.response.ResumeResponse;
import saas.com.br.resume_ai_saas.resume.dto.response.ResumeSummaryResponse;
import saas.com.br.resume_ai_saas.resume.mapper.ResumeMapper;
import saas.com.br.resume_ai_saas.resume.service.ResumeService;
import saas.com.br.resume_ai_saas.security.AuthenticatedUser;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class  ResumeController {

    private static final Duration PRESIGN_TTL = Duration.ofMinutes(5);

    private final ResumeService resumeService;

    @GetMapping
    public ResponseEntity<PageResponse<ResumeSummaryResponse>> findByUser(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID userId = AuthenticatedUser.getId();
        Page<ResumeSummaryResponse> page = resumeService.findByUserId(userId, pageable)
                .map(ResumeMapper::toSummary);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> findById(@PathVariable UUID id) {
        UUID userId = AuthenticatedUser.getId();
        return ResponseEntity.ok(ResumeMapper.toResponse(resumeService.findByIdForUser(id, userId)));
    }

    @GetMapping("/{id}/download-url")
    public ResponseEntity<ResumeDownloadResponse> getDownloadUrl(@PathVariable UUID id) throws Exception {
        UUID userId = AuthenticatedUser.getId();
        String url = resumeService.generatePresignedDownloadUrl(id, userId, PRESIGN_TTL);
        return ResponseEntity.ok(new ResumeDownloadResponse(url, PRESIGN_TTL.toSeconds()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponse> upload(@RequestParam("file") MultipartFile file) {
        UUID userId = AuthenticatedUser.getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResumeMapper.toResponse(resumeService.upload(userId, file)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@resumeSecurity.isOwner(#id)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID userId = AuthenticatedUser.getId();
        resumeService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
