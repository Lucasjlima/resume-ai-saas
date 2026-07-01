package saas.com.br.resume_ai_saas.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URL;
import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SupabaseJwtFilter extends OncePerRequestFilter {

    private final JwkProvider jwkProvider;

    public SupabaseJwtFilter(@Value("${supabase.url}") String supabaseUrl) throws Exception {
        this.jwkProvider = new JwkProviderBuilder(new URL(supabaseUrl + "/auth/v1/.well-known/jwks.json"))
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            DecodedJWT decoded = JWT.decode(token);
            Jwk jwk = jwkProvider.get(decoded.getKeyId());

            Algorithm algorithm = Algorithm.ECDSA256((ECPublicKey) jwk.getPublicKey(), null);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT verified = verifier.verify(token);

            String userId = verified.getSubject();
            String email  = verified.getClaim("email").asString();
            String role   = verified.getClaim("role").asString();

            var userDetails = new SupabaseUserDetails(userId, email, role, verified);

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userDetails, null, mapRoleToAuthorities(role))
            );

        } catch (JWTVerificationException e) {
            log.warn("JWT inválido: {}", e.getMessage());
            sendUnauthorized(response);
            return;
        } catch (Exception e) {
            log.error("Erro ao verificar JWT: {}", e.getMessage());
            sendUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private List<GrantedAuthority> mapRoleToAuthorities(String role) {
        if (role == null) return List.of();
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"Token inválido ou expirado\"}");
    }
}
