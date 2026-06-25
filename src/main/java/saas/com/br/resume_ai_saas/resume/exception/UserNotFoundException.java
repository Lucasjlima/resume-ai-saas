package saas.com.br.resume_ai_saas.resume.exception;

public class UserNotFoundException extends saas.com.br.resume_ai_saas.user.exception.UserNotFoundException {
    public UserNotFoundException(Long id) {
        super(id);
    }
}
