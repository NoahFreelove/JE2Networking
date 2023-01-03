package JE.Networking;

public interface NetworkingObject {
    boolean hasQueued = false;
    void queueNetworkSend();
}
