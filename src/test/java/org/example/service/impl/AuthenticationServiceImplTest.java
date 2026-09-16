package org.example.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.example.configuration.metrics.service.ApplicationMetricsService;
import org.example.dto.request.RefreshTokenRequestDto;
import org.example.dto.request.UserLoginRequestDto;
import org.example.dto.response.UserLoginResponseDto;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.entity.role.RoleName;
import org.example.exception.AuthenticationException;
import org.example.mapper.UserMapper;
import org.example.repository.ChatRepository;
import org.example.repository.RoleRepository;
import org.example.repository.UserRepository;
import org.example.security.audit.SecurityAuditService;
import org.example.security.jwt.JwtUtil;
import org.example.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisService redisService;

    @Mock
    private SecurityAuditService securityAuditService;

    @Mock
    private ApplicationMetricsService metricsService;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private User user;

    @BeforeEach
    void setUp() {

        Role userRole = new Role();
        userRole.setRoleName(RoleName.USER);

        user = new User();
        user.setId(5L);
        user.setEmail("user@example.com");
        user.setPassword("hashed-password");
        user.setRoles(Set.of(userRole));
    }

    @Test
    void login_validCredentials_returnsAccessAndRefreshToken() {

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtUtil.generateToken(eq("user@example.com"), any())).thenReturn("jwt-token");

        UserLoginResponseDto response = authenticationService.login(
                new UserLoginRequestDto("user@example.com", "password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isNotBlank();

        verify(redisService).setValue(anyString(), eq("5"), eq(Duration.ofDays(7)));
    }

    @Test
    void login_unknownEmail_throwsAuthenticationException() {

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(
                new UserLoginRequestDto("ghost@example.com", "password123")))
                .isInstanceOf(AuthenticationException.class);

        verify(redisService, never()).setValue(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void login_wrongPassword_throwsAuthenticationExceptionAndRecordsFailure() {

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.login(
                new UserLoginRequestDto("user@example.com", "wrong-password")))
                .isInstanceOf(AuthenticationException.class);

        verify(securityAuditService).failedLogin("user@example.com", "Bad credentials!");
        verify(metricsService).incrementLoginFailed("bad_credentials");
    }

    @Test
    void refresh_validToken_rotatesItAndReturnsNewPair() {

        when(redisService.getValue(anyString())).thenReturn("5");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(eq("user@example.com"), any())).thenReturn("new-jwt-token");

        UserLoginResponseDto response = authenticationService.refresh(
                new RefreshTokenRequestDto("some-refresh-token"));

        assertThat(response.token()).isEqualTo("new-jwt-token");
        assertThat(response.refreshToken()).isNotBlank();

        verify(redisService, times(1)).delete(anyString());
        verify(redisService, times(1)).setValue(anyString(), eq("5"), eq(Duration.ofDays(7)));
    }

    @Test
    void refresh_unknownOrExpiredToken_throwsAuthenticationException() {

        when(redisService.getValue(anyString())).thenReturn(null);

        assertThatThrownBy(() -> authenticationService.refresh(
                new RefreshTokenRequestDto("stolen-or-expired-token")))
                .isInstanceOf(AuthenticationException.class);

        verify(userRepository, never()).findById(any());
    }

    @Test
    void refresh_userNoLongerExists_throwsAuthenticationException() {

        when(redisService.getValue(anyString())).thenReturn("999");
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.refresh(
                new RefreshTokenRequestDto("token-for-deleted-user")))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void logout_deletesTheStoredRefreshToken() {

        authenticationService.logout(new RefreshTokenRequestDto("some-refresh-token"));

        verify(redisService, times(1)).delete(anyString());
    }
}
