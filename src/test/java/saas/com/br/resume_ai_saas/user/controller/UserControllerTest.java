package saas.com.br.resume_ai_saas.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import saas.com.br.resume_ai_saas.exception.ErrorResponse;
import saas.com.br.resume_ai_saas.exception.GlobalExceptionHandler;
import saas.com.br.resume_ai_saas.exception.UserExceptionHandlerAdvice;
import saas.com.br.resume_ai_saas.user.dto.UpdateUserRequest;
import saas.com.br.resume_ai_saas.user.dto.UserRequest;
import saas.com.br.resume_ai_saas.user.entity.User;
import saas.com.br.resume_ai_saas.user.exception.EmailAlreadyRegisteredException;
import saas.com.br.resume_ai_saas.user.exception.UserNotFoundException;
import saas.com.br.resume_ai_saas.user.service.UserService;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({UserExceptionHandlerAdvice.class, GlobalExceptionHandler.class, ErrorResponse.class})
@DisplayName("UserController MockMvc Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .supabaseUserId("supabase-123")
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
    }

    @Test
    @DisplayName("GET /api/users should return 200 with list of users")
    void getAll_shouldReturn200WithListOfUsers() throws Exception {
        when(userService.findAll()).thenReturn(List.of(testUser));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].email").value("john@example.com"));
    }

    @Test
    @DisplayName("GET /api/users should return 200 with empty list when no users")
    void getAll_shouldReturn200WithEmptyList() throws Exception {
        when(userService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/users/{id} should return 200 with user when found")
    void getById_shouldReturn200_whenUserFound() throws Exception {
        when(userService.findById(1L)).thenReturn(testUser);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.supabaseUserId").value("supabase-123"));
    }

    @Test
    @DisplayName("GET /api/users/{id} should return 404 when user not found")
    void getById_shouldReturn404_whenUserNotFound() throws Exception {
        when(userService.findById(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("99")));
    }

    @Test
    @DisplayName("POST /api/users should return 201 with created user when valid body")
    void create_shouldReturn201_whenValidBody() throws Exception {
        UserRequest request = new UserRequest("John Doe", "john@example.com", "supabase-123");
        when(userService.create(any(User.class))).thenReturn(testUser);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("POST /api/users should return 400 when name is blank")
    void create_shouldReturn400_whenNameIsBlank() throws Exception {
        UserRequest request = new UserRequest("", "john@example.com", null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/users should return 400 when email is invalid")
    void create_shouldReturn400_whenEmailIsInvalid() throws Exception {
        UserRequest request = new UserRequest("John Doe", "not-a-valid-email", null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/users should return 400 when name is too short")
    void create_shouldReturn400_whenNameIsTooShort() throws Exception {
        UserRequest request = new UserRequest("J", "john@example.com", null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/users should return 409 when email is already registered")
    void create_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        UserRequest request = new UserRequest("John Doe", "john@example.com", null);
        when(userService.create(any(User.class)))
                .thenThrow(new EmailAlreadyRegisteredException("john@example.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("john@example.com")));
    }

    @Test
    @DisplayName("PUT /api/users/{id} should return 200 with updated user")
    void update_shouldReturn200_withUpdatedUser() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("Updated Name", "updated@example.com");
        User updatedUser = User.builder()
                .id(1L)
                .name("Updated Name")
                .email("updated@example.com")
                .createdAt(testUser.getCreatedAt())
                .build();
        when(userService.update(eq(1L), eq("Updated Name"), eq("updated@example.com")))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} should return 204")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).delete(1L);
    }

    @Test
    @DisplayName("DELETE /api/users/{id} should return 404 when user not found")
    void delete_shouldReturn404_whenUserNotFound() throws Exception {
        doThrow(new UserNotFoundException(99L)).when(userService).delete(99L);

        mockMvc.perform(delete("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
