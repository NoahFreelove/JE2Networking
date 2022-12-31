package org.networking.Run;

import org.networking.Client;

import java.io.IOException;
import java.net.Socket;

public class ClientTest {
    public static void main(String[] args) {
        try {
            new Client(new Socket("localhost", 6969), "User");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
