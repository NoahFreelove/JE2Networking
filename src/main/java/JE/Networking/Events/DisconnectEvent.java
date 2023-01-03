package JE.Networking.Events;

public interface DisconnectEvent {
    void onDisconnect(DisconnectReason reason);
}
