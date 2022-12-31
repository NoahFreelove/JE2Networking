package org.networking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    public ArrayList<Command> commands = new ArrayList<Command>(Arrays.stream(new Command[]{
            new Command(Role.CLIENT, "disconnectFromServer"),
            new Command(Role.CLIENT, "hello"),
            new Command(Role.CLIENT, "setUsername"),
            new Command(Role.SERVER, "sendTo"),
            new Command(Role.SERVER, "sendToAll"),

    }).toList());

    ServerSocket serverSocket;
    volatile CopyOnWriteArrayList<Client> clients = new CopyOnWriteArrayList<Client>();

    public void open(int port) {
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port));

            Thread inputThread = new Thread(this::userInput);
            inputThread.start();

            Thread queue = new Thread(() -> {
                while (true) {
                    try {
                        Socket s = serverSocket.accept();
                        Client newClient = new Client(s, "user");
                        newClient.connectedServer = this;
                        newClient.connectedSocket = s;
                        newClient.ID = clients.size();
                        newClient.isServerProp = true;
                        System.out.println("New Client: " + newClient.connectedSocket.getInetAddress().getHostAddress());

                        sendClientServerInfo(newClient);

                        clients.add(newClient);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            System.out.println("Now accepting incoming connections on port: " + port + ", " + serverSocket.getInetAddress().getHostAddress());
            System.out.println("---Commands---");
            commands.forEach(command -> System.out.println(command.command() + " : " + command.role()));
            System.out.println("---End Commands---");

            queue.start();
        }
        catch (Exception e) {
            System.out.println("Error: " + e);
        }

    }

    private void userInput() {
        while (true){

            Scanner scanner = new Scanner(System.in);
            String message = scanner.nextLine();
            String[] split = message.split(",");
            if(split.length == 0)
                continue;

            try {

                switch (split[0]) {
                    case "sendToAll" -> {
                        clients.forEach(client -> {
                            try {
                                client.send(serverSocket.getInetAddress().getHostAddress() + "," + split[1]);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                    case "sendTo" -> {
                        if(Integer.parseInt(split[1]) > clients.size())
                            throw new Exception("Client does not exist");
                        clients.get(Integer.parseInt(split[1])).send(serverSocket.getInetAddress().getHostAddress() + "," +split[2]);
                    }
                    default -> System.out.println("Unknown command");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendClientServerInfo(Client c){
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < commands.size(); i++) {
                if(commands.get(i).role().ordinal() == 0){
                    sb.append(commands.get(i).command());
                    if(i != commands.size() - 1){
                        sb.append(",");
                    }
                }
            }

            c.out.writeUTF(serverSocket.getInetAddress().getHostAddress() + ",commands," + sb);
            c.out.writeUTF(serverSocket.getInetAddress().getHostAddress() + ",id," + c.ID);

        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
    }

    public void readClientInput(String command, Client c){
        String[] commandSplit = command.split(",");

        // Check if first command is an ip
        /*if(commandSplit[0].matches("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")) {
            System.out.println("Came from valid IP");
        }*/
        if(commandSplit[0] == null)
            commandSplit = new String[]{""};

        switch (commandSplit[0]) {
            case "setUsername" -> {
                c.username = commandSplit[1];
                System.out.println("Client " + c.ID + " is now known as " + c.username);
                return;
            }
            case "disconnect" -> {
                try {
                    c.connectedSocket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket (disconnecting) with client: " + c.ID);
                }
                clients.remove(c);
                return;
            }
        }
        System.out.println("From " + c.username + ":" + command);
    }

    public void receiveCommand(String command){

    }
}
