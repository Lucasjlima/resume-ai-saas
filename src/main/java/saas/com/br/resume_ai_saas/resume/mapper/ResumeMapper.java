package saas.com.br.resume_ai_saas.resume.mapper;

import saas.com.br.resume_ai_saas.resume.dto.response.ResumeResponse;
import saas.com.br.resume_ai_saas.resume.entity.ExtractionMethod;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.user.entity.User;

import java.time.Instant;

public final class ResumeMapper {

    private ResumeMapper() {}

    public static ResumeResponse toResponse(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getUser().getId(),
                resume.getFileName(),
                resume.getFileUrl(),
                resume.getRawText(),
                resume.getExtractionMethod(),
                resume.getCreatedAt()
        );
    }

    public static Resume toEntity(User user, String fileName, String fileUrl, String rawText) {
        return Resume.builder()
                .user(user)
                .fileName(fileName)
                .fileUrl(fileUrl)
                .rawText(rawText)
                .extractionMethod(ExtractionMethod.PDF_PARSER)
                .createdAt(Instant.now())
                .build();
    }
}
