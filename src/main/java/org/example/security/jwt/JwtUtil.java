package org.example.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import org.example.exception.InvalidJwtTokenException;
import org.example.exception.JwtTokenExpiredException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    private final Key secret;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secretString, @Value("${jwt.expiration}") long expiration) {
        this.secret = Keys.hmacShaKeyFor((secretString.getBytes(StandardCharsets.UTF_8)));
        this.expiration = expiration;
    }

    public String generateToken(String email, List<String> roles) {
        return Jwts.builder().setSubject(email).claim("roles", roles).setIssuedAt(new Date((System.currentTimeMillis()))).setExpiration(new Date(System.currentTimeMillis() + expiration)).signWith(secret).compact();
    }

    public String getUsernameFromToken(String token) {

        return getClaimFromToken(token, Claims::getSubject);
    }

    public List<String> getRolesFomToken(String token) {
        return getClaimFromToken(token, claims -> {
            List<?> roles = claims.get("roles", List.class);

            return roles.stream()
                    .map(Object::toString)
                    .toList();
        });
    }

    public boolean isValidToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secret)
                    .build()
                    .parseClaimsJws(token);
            return true;

        } catch (ExpiredJwtException exception) {
            throw new JwtTokenExpiredException("JWT token expired!");

        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidJwtTokenException("Invalid JWT token!");
        }
    }

    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(token).getBody();
        return claimsResolver.apply(claims);
    }
}
