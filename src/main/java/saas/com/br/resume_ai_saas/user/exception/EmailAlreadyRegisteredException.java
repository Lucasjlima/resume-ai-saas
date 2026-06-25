package saas.com.br.resume_ai_saas.user.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException(String email) {
        super("Email already registered: " + email);
    }
}
