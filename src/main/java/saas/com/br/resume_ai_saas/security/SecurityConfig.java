package saas.com.br.resume_ai_saas.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final String supabaseUrl;
    private final SecurityEntryPoints securityEntryPoints;

    public SecurityConfig(
            @Value("${supabase.url}") String supabaseUrl,
            SecurityEntryPoints securityEntryPoints
    ) {
        this.supabaseUrl = supabaseUrl;
        this.securityEntryPoints = securityEntryPoints;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        SupabaseJwtFilter supabaseJwtFilter = new SupabaseJwtFilter(supabaseUrl);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(securityEntryPoints.authenticationEntryPoint())
                        .accessDeniedHandler(securityEntryPoints.accessDeniedHandler()))
                .addFilterBefore(supabaseJwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
