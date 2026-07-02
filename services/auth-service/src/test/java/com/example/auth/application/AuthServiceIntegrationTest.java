package com.example.auth.application;

import com.example.auth.application.dto.*;
import com.example.auth.application.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("AuthService integration tests")
class AuthServiceIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18.4-alpine")
                    .withDatabaseName("auth_db")
                    .withUsername("postgres")
                    .withPassword("secret");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AuthService authService;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setupRedisMock() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(null);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
    }

    @Test
    @DisplayName("register() — creates user and returns token pair")
    void register_createsUserAndReturnsTokens() {
        TokenResponse response = authService.register(RegisterRequest.builder()
                .email("newuser@example.com")
                .password("Password@123")
                .fullName("New User")
                .build());

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900L);
    }

    @Test
    @DisplayName("register() — duplicate email throws IllegalArgumentException")
    void register_duplicateEmail_throws() {
        RegisterRequest request = RegisterRequest.builder()
                .email("duplicate@example.com")
                .password("Password@123")
                .fullName("User")
                .build();

        authService.register(request);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> authService.register(request));
    }

    @Test
    @DisplayName("login() — valid credentials returns token pair")
    void login_validCredentials_returnsTokens() {
        authService.register(RegisterRequest.builder()
                .email("logintest@example.com")
                .password("Password@123")
                .fullName("Login Test")
                .build());

        TokenResponse response = authService.login(LoginRequest.builder()
                .email("logintest@example.com")
                .password("Password@123")
                .build());

        assertThat(response.getAccessToken()).isNotBlank();
    }

    @Test
    @DisplayName("login() — wrong password throws BadCredentialsException")
    void login_wrongPassword_throws() {
        authService.register(RegisterRequest.builder()
                .email("wrongpw@example.com")
                .password("Password@123")
                .fullName("User")
                .build());

        assertThatExceptionOfType(BadCredentialsException.class)
                .isThrownBy(() -> authService.login(LoginRequest.builder()
                        .email("wrongpw@example.com")
                        .password("WrongPassword")
                        .build()));
    }
}
