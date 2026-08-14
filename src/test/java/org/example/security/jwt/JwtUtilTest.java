package org.example.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import java.util.List;
import org.example.exception.InvalidJwtTokenException;
import org.example.exception.JwtTokenExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-for-jwt-must-be-long-enough-for-hs256!!";
    private static final String OTHER_SECRET = "a-completely-different-secret-key-also-long-enough-hs256";
    private static final long EXPIRATION_MS = 60_000L;
    private static final String EMAIL = "user@example.com";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateToken_thenGetUsername_returnsOriginalEmail() {
        String token = jwtUtil.generateToken(EMAIL, List.of("USER"));

        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo(EMAIL);
    }

    @Test
    void generateToken_thenGetRoles_returnsOriginalRoles() {
        String token = jwtUtil.generateToken(EMAIL, List.of("USER", "ADMIN"));

        assertThat(jwtUtil.getRolesFomToken(token)).containsExactly("USER", "ADMIN");
    }

    @Test
    void isValidToken_freshToken_returnsTrue() {
        String token = jwtUtil.generateToken(EMAIL, List.of("USER"));

        assertThat(jwtUtil.isValidToken(token)).isTrue();
    }

    @Test
    void isValidToken_expiredToken_throwsJwtTokenExpiredException() {
        JwtUtil expiredJwtUtil = new JwtUtil(SECRET, -1000L);
        String token = expiredJwtUtil.generateToken(EMAIL, List.of("USER"));

        assertThatThrownBy(() -> expiredJwtUtil.isValidToken(token))
                .isInstanceOf(JwtTokenExpiredException.class);
    }

    @Test
    void isValidToken_tamperedSignature_throwsInvalidJwtTokenException() {
        String token = jwtUtil.generateToken(EMAIL, List.of("USER"));
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtUtil.isValidToken(tampered))
                .isInstanceOf(InvalidJwtTokenException.class);
    }

    @Test
    void isValidToken_signedWithDifferentSecret_throwsInvalidJwtTokenException() {
        JwtUtil otherJwtUtil = new JwtUtil(OTHER_SECRET, EXPIRATION_MS);
        String token = otherJwtUtil.generateToken(EMAIL, List.of("USER"));

        assertThatThrownBy(() -> jwtUtil.isValidToken(token))
                .isInstanceOf(InvalidJwtTokenException.class);
    }

    @Test
    void isValidToken_malformedToken_throwsInvalidJwtTokenException() {
        assertThatThrownBy(() -> jwtUtil.isValidToken("not-a-jwt-token"))
                .isInstanceOf(InvalidJwtTokenException.class);
    }

    @Test
    void getUsernameFromToken_expiredToken_throwsExpiredJwtException() {
        JwtUtil expiredJwtUtil = new JwtUtil(SECRET, -1000L);
        String token = expiredJwtUtil.generateToken(EMAIL, List.of("USER"));

        assertThatThrownBy(() -> expiredJwtUtil.getUsernameFromToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
