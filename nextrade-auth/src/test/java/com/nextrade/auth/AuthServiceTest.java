package com.nextrade.auth;

import com.nextrade.auth.dto.LoginRequest;
import com.nextrade.auth.dto.RegisterRequest;
import com.nextrade.auth.repository.UserRepository;
import com.nextrade.auth.service.AuthService;
import com.nextrade.common.exception.ConflictException;
import com.nextrade.common.exception.UnauthorizedException;
import com.nextrade.common.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.nextrade.auth.repository.RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Register should throw ConflictException when email already exists")
    void register_shouldThrowConflict_whenEmailExists() {
        var request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");
        request.setFirstName("Test");
        request.setLastName("User");

        org.mockito.Mockito.when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Email already registered");
    }

    @Test
    @DisplayName("Login should throw UnauthorizedException for invalid credentials")
    void login_shouldThrowUnauthorized_whenInvalidCredentials() {
        var request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("wrongpassword");

        org.mockito.Mockito.when(userRepository.findByEmail("unknown@example.com"))
            .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Invalid credentials");
    }
}
