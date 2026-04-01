package com.nextrade.auth.config;

import com.nextrade.common.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtHeaderFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Filter that extracts user info from gateway-injected headers
    public static class JwtHeaderFilter extends OncePerRequestFilter {
        private final JwtUtil jwtUtil;

        public JwtHeaderFilter(JwtUtil jwtUtil) {
            this.jwtUtil = jwtUtil;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            // Auth service can also validate JWT directly (for direct access)
            String authHeader = request.getHeader("Authorization");
            String userIdHeader = request.getHeader("X-User-Id");
            String roleHeader = request.getHeader("X-User-Role");
            String emailHeader = request.getHeader("X-User-Email");

            if (userIdHeader != null && roleHeader != null && emailHeader != null) {
                // Request came through gateway - trust the headers
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roleHeader));
                var auth = new UsernamePasswordAuthenticationToken(emailHeader, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    if (jwtUtil.isTokenValid(token) && jwtUtil.isAccessToken(token)) {
                        String email = jwtUtil.extractEmail(token);
                        String role = jwtUtil.extractRole(token);
                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                        var auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        // Inject headers for consistency
                        request = wrapWithHeaders(request, jwtUtil.extractUserId(token), email, role);
                    }
                } catch (Exception e) {
                    log.debug("JWT filter error: {}", e.getMessage());
                }
            }

            filterChain.doFilter(request, response);
        }

        private HttpServletRequest wrapWithHeaders(HttpServletRequest request, Long userId, String email, String role) {
            return new jakarta.servlet.http.HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("X-User-Id".equals(name)) return String.valueOf(userId);
                    if ("X-User-Email".equals(name)) return email;
                    if ("X-User-Role".equals(name)) return role;
                    return super.getHeader(name);
                }
            };
        }
    }
}
