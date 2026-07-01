package saas.com.br.resume_ai_saas.exception;

public class ResumeNotFoundException extends RuntimeException {
    public ResumeNotFoundException(Long id) {
        super("Resume not found with id: " + id);
    }
}
