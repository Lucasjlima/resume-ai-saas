package saas.com.br.resume_ai_saas.exception;

import java.util.UUID;

public class ResumeNotFoundException extends RuntimeException {
    public ResumeNotFoundException(UUID id) {
        super("Resume not found with id: " + id);
    }
}
