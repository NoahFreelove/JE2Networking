package org.networking.Run;

import org.networking.Server;

public class ServerTest {
    public static void main(String[] args) {
        Server s = new Server();
        s.open(6969);
    }
}