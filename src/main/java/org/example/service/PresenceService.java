package org.example.service;

public interface PresenceService {

    void heartbeat();

    void updateLastSeen();

    void disconnect();
}
