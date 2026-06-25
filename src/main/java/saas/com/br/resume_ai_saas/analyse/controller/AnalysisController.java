package saas.com.br.resume_ai_saas.analyse.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import saas.com.br.resume_ai_saas.analyse.dto.request.AnalysisRequest;
import saas.com.br.resume_ai_saas.analyse.dto.response.AnalysisResponse;
import saas.com.br.resume_ai_saas.analyse.mapper.AnalysisMapper;
import saas.com.br.resume_ai_saas.analyse.service.AnalysisService;

import java.util.List;

@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping
    public ResponseEntity<List<AnalysisResponse>> findByResume(@RequestParam Long resumeId) {
        List<AnalysisResponse> responses = analysisService.findByResumeId(resumeId).stream()
                .map(AnalysisMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnalysisResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(AnalysisMapper.toResponse(analysisService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<AnalysisResponse> analyze(@RequestParam Long resumeId,
                                                     @Valid @RequestBody AnalysisRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AnalysisMapper.toResponse(
                        analysisService.analyze(resumeId, request.jobDescription())
                ));
    }
}
