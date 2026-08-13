package org.example.security.audit;

public interface SecurityAuditService {

    void failedLogin(String email, String reason);

    void invalidJwtToken(String tokenHint, String reason);

    void webSocketDenied(Long userId, String destination, String reason);

    void forbiddenChatAccess(Long userId, Long chatId);

    void rateLimitExceeded(String ip, String endpoint);
}
