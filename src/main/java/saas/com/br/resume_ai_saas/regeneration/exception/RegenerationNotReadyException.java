package saas.com.br.resume_ai_saas.regeneration.exception;

import java.util.UUID;

public class RegenerationNotReadyException extends RuntimeException {
    public RegenerationNotReadyException(UUID id) {
        super("Regeneration " + id + " has no PDF available for download yet");
    }
}
