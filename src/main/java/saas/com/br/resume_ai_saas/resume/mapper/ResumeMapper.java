package saas.com.br.resume_ai_saas.resume.mapper;

import saas.com.br.resume_ai_saas.resume.dto.response.ResumeResponse;
import saas.com.br.resume_ai_saas.resume.entity.ExtractionMethod;
import saas.com.br.resume_ai_saas.resume.entity.Resume;

import java.time.Instant;
import java.util.UUID;

public final class ResumeMapper {

    private ResumeMapper() {}

    public static ResumeResponse toResponse(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getUserId(),
                resume.getFileName(),
                resume.getFileUrl(),
                resume.getRawText(),
                resume.getExtractionMethod(),
                resume.getCreatedAt()
        );
    }

    public static Resume toEntity(UUID userId, String fileName, String fileUrl, String rawText) {
        return Resume.builder()
                .userId(userId)
                .fileName(fileName)
                .fileUrl(fileUrl)
                .rawText(rawText)
                .extractionMethod(ExtractionMethod.PDF_PARSER)
                .createdAt(Instant.now())
                .build();
    }
}
