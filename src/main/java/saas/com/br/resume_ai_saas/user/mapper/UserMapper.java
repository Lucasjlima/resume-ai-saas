package saas.com.br.resume_ai_saas.user.mapper;

import saas.com.br.resume_ai_saas.user.dto.request.UserRequest;
import saas.com.br.resume_ai_saas.user.dto.response.UserResponse;
import saas.com.br.resume_ai_saas.user.entity.User;
import java.time.Instant;

public final class UserMapper {

    private UserMapper() {}

    public static User toEntity(UserRequest request) {
        return User.builder()
                .name(request.name())
                .email(request.email())
                .supabaseUserId(request.supabaseUserId())
                .createdAt(Instant.now())
                .build();
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getSupabaseUserId(),
                user.getCreatedAt()
        );
    }
}
