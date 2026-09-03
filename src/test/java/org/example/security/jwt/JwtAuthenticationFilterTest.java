package org.example.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.example.configuration.metrics.service.ApplicationMetricsService;
import org.example.entity.User;
import org.example.exception.InvalidJwtTokenException;
import org.example.exception.JwtTokenExpiredException;
import org.example.security.CustomUserDetailsService;
import org.example.security.UserSecurity;
import org.example.security.audit.SecurityAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "some.jwt.token";
    private static final String EMAIL = "user@example.com";

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private SecurityAuditService securityAuditService;

    @Mock
    private ApplicationMetricsService metricsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_validToken_authenticatesAndContinuesChain() throws Exception {
        User user = new User();
        user.setEmail(EMAIL);
        when(jwtUtil.getUsernameFromToken(TOKEN)).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(new UserSecurity(user));

        MockHttpServletRequest request = requestWithToken(TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void doFilter_expiredToken_returnsCleanUnauthorizedInsteadOfPropagating() throws Exception {
        doThrow(new JwtTokenExpiredException("JWT token expired!"))
                .when(jwtUtil).validateToken(TOKEN);

        MockHttpServletRequest request = requestWithToken(TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
        verify(securityAuditService).invalidJwtToken(anyString(), anyString());
    }

    @Test
    void doFilter_invalidSignature_returnsCleanUnauthorizedInsteadOfPropagating() throws Exception {
        doThrow(new InvalidJwtTokenException("Invalid JWT token!"))
                .when(jwtUtil).validateToken(TOKEN);

        MockHttpServletRequest request = requestWithToken(TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilter_noAuthorizationHeader_continuesChainUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
