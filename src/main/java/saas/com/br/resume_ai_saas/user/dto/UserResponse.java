package saas.com.br.resume_ai_saas.user.dto;

import java.time.Instant;

public record UserResponse(
    Long id,
    String name,
    String email,
    String supabaseUserId,
    Instant createdAt
) {}
