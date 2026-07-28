package org.example.security;

import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.example.entity.User;
import org.example.exception.AuthenticationException;
import org.example.exception.UserNotFoundException;
import org.example.repository.UserRepository;
import org.example.security.websocket.model.WebSocketPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UserSecurity userSecurity)) {
            throw new AuthenticationException("Authentication is missing!");
        }

        return userSecurity.getUser();
    }

    public User getAuthenticatedUser(Principal principal) {

        if (principal instanceof WebSocketPrincipal ws) {
            return userRepository.findById(ws.userId())
                    .orElseThrow(() -> new UserNotFoundException("User not found!"));
        }

        String actualType = principal == null ? "null" : principal.getClass().getName();
        throw new AuthenticationException("Unsupported principal type: " + actualType);
    }
}
