package org.example.global;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.example.exception.AuthenticationException;
import org.example.exception.BadRequestException;
import org.example.exception.ChatNotFoundException;
import org.example.exception.ForbiddenActionException;
import org.example.exception.InvalidJwtTokenException;
import org.example.exception.JwtTokenExpiredException;
import org.example.exception.MessageNotFoundException;
import org.example.exception.RegistrationException;
import org.example.exception.UserNotFoundException;
import org.example.exception.UserRoleNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd 'T' HH:mm:ss");

    private static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";

    public record ErrorResponseBody(
            String timestamp,
            String error,
            String code,
            String traceId
    ) {
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        List<String> errors = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> Optional.ofNullable(error.getDefaultMessage())
                        .orElse("Validation error!"))
                .toList();

        String message = errors.isEmpty() ? "Validation is failed!" : errors.get(0);

        String traceId = UUID.randomUUID().toString();

        ErrorResponseBody response = new ErrorResponseBody(
                LocalDateTime.now().format(formatter),
                message,
                VALIDATION_ERROR_CODE,
                traceId
        );

        log.error("Error [{}]: {} (code={})", traceId, message, VALIDATION_ERROR_CODE);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<ErrorResponseBody> handleRegistrationException(
            RegistrationException exception) {

        return buildResponse(exception.getMessage(), "REGISTRATION_FAILED",
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseBody> handleAuthenticationException(
            AuthenticationException exception) {

        return buildResponse(exception.getMessage(), "AUTHENTICATION_FAILED",
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<ErrorResponseBody> handleForbiddenActionException(
            ForbiddenActionException exception) {

        return buildResponse(exception.getMessage(), "FORBIDDEN", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponseBody> handleBadRequestException(
            BadRequestException exception) {

        return buildResponse(exception.getMessage(), "BAD_REQUEST",
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            UserNotFoundException.class,
            UserRoleNotFoundException.class,
            ChatNotFoundException.class,
            MessageNotFoundException.class
    })
    public ResponseEntity<ErrorResponseBody> handleNotFoundException(
            RuntimeException exception) {

        return buildResponse(exception.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidJwtTokenException.class)
    public ResponseEntity<ErrorResponseBody> handleInvalidJwtTokenException(
            InvalidJwtTokenException exception) {

        return buildResponse(exception.getMessage(), "INVALID_JWT",
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(JwtTokenExpiredException.class)
    public ResponseEntity<ErrorResponseBody> handleExpiredJwtException(
            JwtTokenExpiredException exception) {

        return buildResponse(exception.getMessage(), "JWT_EXPIRED",
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseBody> handleGlobalException(Exception exception) {

        log.error("Unexpected error: {}", exception.getMessage(), exception);

        return buildResponse("Internal server error", "INTERNAL_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseBody> handleIllegalStateException(
            IllegalStateException exception) {

        return buildResponse(exception.getMessage(), "ILLEGAL_STATE",
                HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ErrorResponseBody> buildResponse(String message, String code,
                                                            HttpStatus status) {

        String traceId = UUID.randomUUID().toString();

        ErrorResponseBody response = new ErrorResponseBody(
                LocalDateTime.now().format(formatter),
                message,
                code,
                traceId
        );

        if (status.is5xxServerError()) {
            log.error("Error [{}]: {} (code={})", traceId, message, code);
        } else {
            log.warn("Error [{}]: {} (code={})", traceId, message, code);
        }

        return ResponseEntity.status(status).body(response);
    }
}
