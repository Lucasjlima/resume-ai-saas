package saas.com.br.resume_ai_saas.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import saas.com.br.resume_ai_saas.user.dto.UpdateUserRequest;
import saas.com.br.resume_ai_saas.user.dto.UserRequest;
import saas.com.br.resume_ai_saas.user.dto.UserResponse;
import saas.com.br.resume_ai_saas.user.mapper.UserMapper;
import saas.com.br.resume_ai_saas.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> findAll() {
        List<UserResponse> responses = userService.findAll().stream()
                .map(UserMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(UserMapper.toResponse(userService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserMapper.toResponse(userService.create(UserMapper.toEntity(request))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(
                UserMapper.toResponse(userService.update(id, request.name(), request.email()))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
