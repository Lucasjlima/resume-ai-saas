package saas.com.br.resume_ai_saas.resume.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import saas.com.br.resume_ai_saas.resume.dto.response.ResumeResponse;
import saas.com.br.resume_ai_saas.resume.mapper.ResumeMapper;
import saas.com.br.resume_ai_saas.resume.service.ResumeService;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    public ResponseEntity<List<ResumeResponse>> findByUser(@RequestParam Long userId) {
        List<ResumeResponse> responses = resumeService.findByUserId(userId).stream()
                .map(ResumeMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ResumeMapper.toResponse(resumeService.findById(id)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponse> upload(@RequestParam Long userId,
                                                  @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResumeMapper.toResponse(resumeService.upload(userId, file)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resumeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
