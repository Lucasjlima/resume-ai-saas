package saas.com.br.resume_ai_saas.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import saas.com.br.resume_ai_saas.resume.repository.ResumeRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeSecurity ownership checks")
class ResumeSecurityTest {

    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private ResumeSecurity resumeSecurity;

    private final UUID userId = UUID.randomUUID();
    private final UUID resumeId = UUID.randomUUID();

    @BeforeEach
    void authenticateUser() {
        var principal = new SupabaseUserDetails(userId.toString(), "user@example.com", "authenticated", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("returns true when the authenticated user owns an active resume")
    void isOwner_true_whenUserOwnsActiveResume() {
        when(resumeRepository.existsByIdAndUserIdAndDeletedAtIsNull(resumeId, userId)).thenReturn(true);

        assertThat(resumeSecurity.isOwner(resumeId)).isTrue();
    }

    @Test
    @DisplayName("returns false when the resume belongs to another user (IDOR attempt)")
    void isOwner_false_whenResumeBelongsToAnotherUser() {
        when(resumeRepository.existsByIdAndUserIdAndDeletedAtIsNull(resumeId, userId)).thenReturn(false);

        assertThat(resumeSecurity.isOwner(resumeId)).isFalse();
    }

    @Test
    @DisplayName("returns false for a null id without hitting the repository")
    void isOwner_false_whenIdNull() {
        assertThat(resumeSecurity.isOwner(null)).isFalse();
        verifyNoInteractions(resumeRepository);
    }
}
