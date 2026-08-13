package org.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.configuration.metrics.service.ApplicationMetricsService;
import org.example.security.audit.SecurityAuditService;
import org.example.service.RedisService;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/auth/authentication";
    private static final String REGISTRATION_PATH = "/auth/registration";

    private static final int LOGIN_LIMIT = 5;
    private static final Duration LOGIN_WINDOW = Duration.ofSeconds(60);

    private static final int REGISTRATION_LIMIT = 3;
    private static final Duration REGISTRATION_WINDOW = Duration.ofMinutes(10);

    private final RedisService redisService;

    private final SecurityAuditService securityAuditService;

    private final ApplicationMetricsService metricsService;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {

        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        return !LOGIN_PATH.equals(path) && !REGISTRATION_PATH.equals(path);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = request.getRemoteAddr();

        int limit = LOGIN_PATH.equals(path) ? LOGIN_LIMIT : REGISTRATION_LIMIT;
        Duration window = LOGIN_PATH.equals(path) ? LOGIN_WINDOW : REGISTRATION_WINDOW;

        String key = "ratelimit:auth:" + path + ":" + ip;

        if (!redisService.isAllowed(key, limit, window)) {

            securityAuditService.rateLimitExceeded(ip, path);
            metricsService.incrementRateLimitExceeded(path);

            log.warn("Rate limit exceeded: ip={}, path={}", ip, path);
            sendTooManyRequestsResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendTooManyRequestsResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\": \"Too many requests, try again later\"}");
    }
}
