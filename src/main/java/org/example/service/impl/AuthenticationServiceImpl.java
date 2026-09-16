package org.example.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.example.configuration.metrics.service.ApplicationMetricsService;
import org.example.dto.request.RefreshTokenRequestDto;
import org.example.dto.request.UserLoginRequestDto;
import org.example.dto.request.UserRegistrationRequestDto;
import org.example.dto.response.UserLoginResponseDto;
import org.example.dto.response.UserRegistrationResponseDto;
import org.example.entity.Chat;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.entity.role.RoleName;
import org.example.entity.type.ChatType;
import org.example.exception.AuthenticationException;
import org.example.exception.RegistrationException;
import org.example.exception.UserRoleNotFoundException;
import org.example.mapper.UserMapper;
import org.example.repository.ChatRepository;
import org.example.repository.RoleRepository;
import org.example.repository.UserRepository;
import org.example.security.audit.SecurityAuditService;
import org.example.security.jwt.JwtUtil;
import org.example.service.AuthenticationService;
import org.example.service.RedisService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final ChatRepository chatRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final RedisService redisService;

    private final SecurityAuditService securityAuditService;

    private final ApplicationMetricsService metricsService;

    @Override
    @Transactional
    public UserRegistrationResponseDto registration(UserRegistrationRequestDto request) {

        String email = request.email().trim().toLowerCase();

        String nickname = request.nickName().trim();

        if (userRepository.existsByEmail(email)) {
            throw new RegistrationException("Email already exists!");
        }

        if (userRepository.existsByNickname(nickname)) {
            throw new RegistrationException("Nickname already exists!");
        }

        Role userRole = roleRepository.findByRoleName(RoleName.USER)
                .orElseThrow(() -> new UserRoleNotFoundException(
                        "The USER role not found!  "));

        User user = userMapper.toEntity(request);

        user.setEmail(email);
        user.setNickname(nickname);
        user.setPassword(passwordEncoder.encode(request.password()));

        user.addRole(userRole);

        User savedUser = userRepository.save(user);

        createSelfChat(savedUser);

        return userMapper.toRegistrationDto(savedUser);
    }

    @Override
    public UserLoginResponseDto login(UserLoginRequestDto request) {

        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("Invalid email or password!"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {

            securityAuditService.failedLogin(email, "Bad credentials!");
            metricsService.incrementLoginFailed("bad_credentials");

            throw new AuthenticationException("Invalid email or password!");
        }

        return issueTokens(user);
    }

    @Override
    public UserLoginResponseDto refresh(RefreshTokenRequestDto request) {

        Long userId = consumeRefreshToken(request.refreshToken());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException(
                        "Invalid or expired refresh token!"));

        return issueTokens(user);
    }

    @Override
    public void logout(RefreshTokenRequestDto request) {

        redisService.delete(REFRESH_TOKEN_PREFIX + hash(request.refreshToken()));
    }

    private UserLoginResponseDto issueTokens(User user) {

        String accessToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRoles().stream().map(role -> role.getRoleName().name()).toList());

        String refreshToken = issueRefreshToken(user.getId());

        return new UserLoginResponseDto(accessToken, refreshToken);
    }

    private String issueRefreshToken(Long userId) {

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        redisService.setValue(REFRESH_TOKEN_PREFIX + hash(rawToken), userId.toString(),
                REFRESH_TOKEN_TTL);

        return rawToken;
    }

    private Long consumeRefreshToken(String rawToken) {

        String key = REFRESH_TOKEN_PREFIX + hash(rawToken);
        String userId = redisService.getValue(key);

        if (userId == null) {
            throw new AuthenticationException("Invalid or expired refresh token!");
        }

        redisService.delete(key);

        return Long.parseLong(userId);
    }

    private String hash(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private void createSelfChat(User savedUser) {

        if (chatRepository.existsByOwnerIdAndChatType(savedUser.getId(), ChatType.SELF)) {
            return;
        }

        Chat selfChat = new Chat();

        selfChat.setChatType(ChatType.SELF);

        selfChat.getParticipants().add(savedUser);

        selfChat.setOwnerId(savedUser.getId());

        selfChat.setLastMessageText(null);

        chatRepository.save(selfChat);
    }
}
