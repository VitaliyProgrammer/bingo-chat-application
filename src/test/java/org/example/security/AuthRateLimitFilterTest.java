package org.example.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import org.example.configuration.metrics.service.ApplicationMetricsService;
import org.example.security.audit.SecurityAuditService;
import org.example.service.RedisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthRateLimitFilterTest {

    private static final String IP = "127.0.0.1";

    @Mock
    private RedisService redisService;

    @Mock
    private SecurityAuditService securityAuditService;

    @Mock
    private ApplicationMetricsService metricsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private AuthRateLimitFilter filter;

    @Test
    void doFilter_getRequest_isNotFiltered() throws Exception {

        when(request.getMethod()).thenReturn("GET");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(redisService, never()).isAllowed(anyString(), anyInt(), any(Duration.class));
    }

    @Test
    void doFilter_nonAuthPath_isNotFiltered() throws Exception {

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/users/search");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(redisService, never()).isAllowed(anyString(), anyInt(), any(Duration.class));
    }

    @Test
    void doFilter_loginWithinLimit_passesThrough() throws Exception {

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/auth/authentication");
        when(request.getRemoteAddr()).thenReturn(IP);
        when(redisService.isAllowed(anyString(), anyInt(), any(Duration.class)))
                .thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_loginOverLimit_returns429AndBlocksChain() throws Exception {

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/auth/authentication");
        when(request.getRemoteAddr()).thenReturn(IP);
        when(redisService.isAllowed(anyString(), anyInt(), any(Duration.class)))
                .thenReturn(false);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, never()).doFilter(any(), any());
        verify(securityAuditService).rateLimitExceeded(IP, "/auth/authentication");
        verify(metricsService).incrementRateLimitExceeded("/auth/authentication");
    }

    @Test
    void doFilter_registrationOverLimit_usesRegistrationLimits() throws Exception {

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/auth/registration");
        when(request.getRemoteAddr()).thenReturn(IP);
        when(redisService.isAllowed(anyString(), anyInt(), any(Duration.class)))
                .thenReturn(false);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilter(request, response, filterChain);

        verify(redisService).isAllowed(
                "ratelimit:auth:/auth/registration:" + IP, 3, Duration.ofMinutes(10));
    }
}
