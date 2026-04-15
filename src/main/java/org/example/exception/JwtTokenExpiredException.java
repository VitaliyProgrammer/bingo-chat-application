package org.example.exception;

public class JwtTokenExpiredException extends RuntimeException {

    public JwtTokenExpiredException(String message) {
        super(message);
    }
}
