package saas.com.br.resume_ai_saas.security;

import com.auth0.jwt.interfaces.DecodedJWT;

public record SupabaseUserDetails(
        String userId,
        String email,
        String role,
        DecodedJWT jwt
) {}
