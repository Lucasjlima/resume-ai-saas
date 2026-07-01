package saas.com.br.resume_ai_saas.resume.mapper;

import saas.com.br.resume_ai_saas.resume.dto.response.ResumeResponse;
import saas.com.br.resume_ai_saas.resume.entity.ExtractionMethod;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.resume.entity.ResumeStatus;

import java.time.Instant;
import java.util.UUID;

public final class ResumeMapper {

    private ResumeMapper() {}

    public static ResumeResponse toResponse(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getUserId(),
                resume.getFileName(),
                resume.getRawText(),
                resume.getStatus(),
                resume.getExtractionMethod(),
                resume.getCreatedAt()
        );
    }

    public static Resume toEntity(UUID userId, String fileName, String rawText) {
        return Resume.builder()
                .userId(userId)
                .fileName(fileName)
                .rawText(rawText)
                .status(ResumeStatus.PROCESSING)
                .extractionMethod(ExtractionMethod.PDF_PARSER)
                .createdAt(Instant.now())
                .build();
    }
}
