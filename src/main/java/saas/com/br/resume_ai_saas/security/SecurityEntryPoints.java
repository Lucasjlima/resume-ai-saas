package saas.com.br.resume_ai_saas.security;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import saas.com.br.resume_ai_saas.exception.ErrorResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityEntryPoints {

    private final ObjectMapper objectMapper;

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> writeError(request, response, HttpStatus.UNAUTHORIZED,
                "Authentication required to access this resource");
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> writeError(request, response, HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource");
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response,
                            HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErrorResponse body = ErrorResponse.of(message, status.value());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
