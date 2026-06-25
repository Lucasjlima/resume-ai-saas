package saas.com.br.resume_ai_saas.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saas.com.br.resume_ai_saas.user.entity.User;
import saas.com.br.resume_ai_saas.user.exception.EmailAlreadyRegisteredException;
import saas.com.br.resume_ai_saas.user.exception.UserNotFoundException;
import saas.com.br.resume_ai_saas.user.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .supabaseUserId("supabase-123")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("findAll() should return a list of all users")
    void findAll_shouldReturnListOfUsers() {
        User anotherUser = User.builder()
                .id(2L)
                .name("Jane Doe")
                .email("jane@example.com")
                .createdAt(Instant.now())
                .build();
        when(userRepository.findAll()).thenReturn(List.of(testUser, anotherUser));

        List<User> result = userService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testUser, anotherUser);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("findAll() should return empty list when no users exist")
    void findAll_shouldReturnEmptyListWhenNoUsersExist() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<User> result = userService.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findById() should return user when found")
    void findById_shouldReturnUser_whenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User result = userService.findById(1L);

        assertThat(result).isEqualTo(testUser);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John Doe");
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("findById() should throw UserNotFoundException when user not found")
    void findById_shouldThrowUserNotFoundException_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("create() should save and return user when email does not exist")
    void create_shouldSaveAndReturnUser_whenEmailNotExists() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userService.create(testUser);

        assertThat(result).isEqualTo(testUser);
        verify(userRepository).existsByEmail("john@example.com");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("create() should throw EmailAlreadyRegisteredException when email already exists")
    void create_shouldThrowEmailAlreadyRegisteredException_whenEmailExists() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(testUser))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessageContaining("john@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("update() should update name and email correctly when no conflicts")
    void update_shouldUpdateNameAndEmail_whenNoConflicts() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userService.update(1L, "New Name", "newemail@example.com");

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getEmail()).isEqualTo("newemail@example.com");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("update() should not check email conflict when email is same as current")
    void update_shouldNotCheckEmailConflict_whenEmailIsSame() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userService.update(1L, "New Name", "john@example.com");

        assertThat(result.getName()).isEqualTo("New Name");
        // existsByEmail should not be called for the same email
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    @DisplayName("update() should throw EmailAlreadyRegisteredException when new email is already used by another user")
    void update_shouldThrowEmailAlreadyRegisteredException_whenNewEmailConflicts() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1L, "John Doe", "taken@example.com"))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessageContaining("taken@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("update() should throw UserNotFoundException when user does not exist")
    void update_shouldThrowUserNotFoundException_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(99L, "Name", "email@example.com"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("update() should only update non-blank fields")
    void update_shouldOnlyUpdateNonBlankFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        userService.update(1L, null, null);

        // Name and email should remain unchanged
        assertThat(testUser.getName()).isEqualTo("John Doe");
        assertThat(testUser.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("delete() should call deleteById when user exists")
    void delete_shouldCallDeleteById_whenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete() should throw UserNotFoundException when user does not exist")
    void delete_shouldThrowUserNotFoundException_whenUserNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(userRepository, never()).deleteById(anyLong());
    }
}
