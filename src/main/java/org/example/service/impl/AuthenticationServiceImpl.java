package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.UserLoginRequestDto;
import org.example.dto.request.UserRegistrationRequestDto;
import org.example.dto.response.UserLoginResponseDto;
import org.example.dto.response.UserRegistrationResponseDto;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.entity.role.RoleName;
import org.example.exception.AuthenticationException;
import org.example.exception.RegistrationException;
import org.example.exception.UserRoleNotFoundException;
import org.example.mapper.UserMapper;
import org.example.repository.RoleRepository;
import org.example.repository.UserRepository;
import org.example.security.audit.SecurityAuditService;
import org.example.security.jwt.JwtUtil;
import org.example.service.AuthenticationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final SecurityAuditService securityAuditService;

    @Override
    @Transactional
    public UserRegistrationResponseDto registration(UserRegistrationRequestDto request) {

        String email = request.email().trim().toLowerCase();

        String nickname = request.nickName().trim();

        if (userRepository.existsByEmail(email)) {
            throw new RegistrationException("Email already exists!");
        }

        if (userRepository.existsByNickName(nickname)) {
            throw new RegistrationException("NickName already exists!");
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

        return userMapper.toRegistrationDto(savedUser);
    }

    @Override
    public UserLoginResponseDto login(UserLoginRequestDto request) {

        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("Invalid email or password!"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {

            securityAuditService.failedLogin(email, "Bad credentials!");
            throw new AuthenticationException("Invalid email or password!");
        }

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRoles().stream().map(role -> role.getRoleName().name()).toList());

        return new UserLoginResponseDto(token);
    }
}
