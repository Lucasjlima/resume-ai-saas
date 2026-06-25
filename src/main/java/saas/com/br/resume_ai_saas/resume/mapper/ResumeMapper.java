package saas.com.br.resume_ai_saas.resume.mapper;

import saas.com.br.resume_ai_saas.resume.dto.ResumeResponse;
import saas.com.br.resume_ai_saas.resume.entity.Resume;

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
}
