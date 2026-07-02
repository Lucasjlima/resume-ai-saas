package saas.com.br.resume_ai_saas.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class AuthenticatedUser {

    public static UUID getId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof SupabaseUserDetails details)) {
            throw new IllegalStateException("Unexpected principal type: " + principal.getClass());
        }
        return UUID.fromString(details.userId());
    }
}
