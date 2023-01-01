package org.networking.Run;

import org.networking.Client;

import java.io.IOException;
import java.net.Socket;

public class ClientTest {
    public static void main(String[] args) {
        try {
            String username = "User";
            if(args.length > 0)
                username = args[0];
            new Client(new Socket("localhost", 6969), username);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
