package JE.networking.Run;

import JE.networking.Server.Server;

public class ServerTest {
    public static void main(String[] args) {
        Server s = new Server();
        s.open(6969);
    }
}