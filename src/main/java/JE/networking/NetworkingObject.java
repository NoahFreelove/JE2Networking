package JE.networking;

public interface NetworkingObject {
    boolean hasQueued = false;
    void queueNetworkSend();
}
