package org.example.security.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SecurityAuditServiceImpl implements SecurityAuditService {

    @Override
    public void failedLogin(String email, String reason) {
        log.warn("SECURITY_AUDIT failed_login email={}, reason={}", email, reason);
    }

    @Override
    public void invalidJwtToken(String tokenHint, String reason) {
        log.warn("SECURITY_AUDIT invalid_jwt_token token={}, reason={}", tokenHint, reason);
    }

    @Override
    public void webSocketDenied(Long userId, String destination, String reason) {
        log.warn("SECURITY_AUDIT websocket_denied userId={}, reason={}, reason={}",
                userId, destination, reason);
    }

    @Override
    public void forbiddenChatAccess(Long userId, Long chatId) {
        log.warn("SECURITY_AUDIT forbidden_chat_access userId={}, reason={}, reason={}",
                userId, chatId);
    }
}
