package org.example.service;

import java.security.Principal;

public interface PresenceService {

    void heartbeat(Principal principal);

    void updateLastSeen(Principal principal);
}
