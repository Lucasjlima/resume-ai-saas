package saas.com.br.resume_ai_saas.resume.dto.response;

public record ResumeDownloadResponse(
    String url,
    long expiresInSeconds
) {}
