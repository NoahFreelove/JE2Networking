package org.networking.Events;

public interface DisconnectEvent {
    void onDisconnect(DisconnectReason reason);
}
