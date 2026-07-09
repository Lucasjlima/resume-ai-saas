package saas.com.br.resume_ai_saas.regeneration.exception;

import java.util.UUID;

public class RegenerationNotFoundException extends RuntimeException {
    public RegenerationNotFoundException(UUID id) {
        super("Regeneration not found with id: " + id);
    }
}
