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

    public Server(){}

    public Server(Command... defaultCommands){
        this.commands = new ArrayList<>(Arrays.asList(defaultCommands));
    }

    volatile CopyOnWriteArrayList<Client> clients = new CopyOnWriteArrayList<>();

    public ArrayList<Command> commands = new ArrayList<>(Arrays.stream(new Command[]{
            new Command(Role.CLIENT, "disconnectFromServer", "none", (args, initiatedBy) -> {
                try {
                    initiatedBy.connectedSocket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket (disconnecting) with client: " + initiatedBy.ID);
                }
                clients.remove(initiatedBy);
            }),

            new Command(Role.CLIENT, "hello", "none", new CommandExec() {
                @Override
                public void execute(String[] args, Client initiatedBy) {
                    serverSendTo(initiatedBy, "Hello " + initiatedBy.ID + "!");
                }
            }),

            new Command(Role.CLIENT, "setUsername", "[Arg1 - new username]", (args, initiatedBy) -> {
                initiatedBy.username = args[0];
                System.out.println("Client " + initiatedBy.ID + " is now known as " + initiatedBy.username);
            }),

            new Command(Role.SERVER, "sendTo", "[Arg1 - client index], [Arg2 - message]", (args, initiatedBy) -> {
                if(Integer.parseInt(args[0]) > clients.size())
                    return;
                serverSendTo(clients.get(Integer.parseInt(args[0])), args[1]);
            }),

            new Command(Role.SERVER, "sendToAll", "[Arg1 - message]", (args, initiatedBy) -> clients.forEach(client -> {
                try {
                    serverSendTo(client, "commands," + args[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            })),
            new Command(Role.CLIENT, "whisper", "[Arg1 - client index], [Arg2 - message]", new CommandExec() {
                @Override
                public void execute(String[] args, Client initiatedBy) {
                    if(Integer.parseInt(args[0]) > clients.size())
                        return;
                    serverSendTo(clients.get(Integer.parseInt(args[0])), "whisper from '" + initiatedBy.username + "':\n" + args[1]);
                }
            }),
            new Command(Role.CLIENT, "sync", "none", new CommandExec() {
                @Override
                public void execute(String[] args, Client initiatedBy) {
                    sendClientServerInfo(initiatedBy);
                }
            }),

    }).toList());

    ServerSocket serverSocket;

    public void open(int port) {
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port));

            Thread inputThread = new Thread(this::serverInput);
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

    private void serverInput() {
        while (true){

            Scanner scanner = new Scanner(System.in);
            String message = scanner.nextLine();
            String[] split = message.split(",");
            if(split.length == 0)
                continue;

            try {
                for (Command c :
                        commands) {
                    if(c.command().equals(split[0]) && c.role() == Role.SERVER){
                        c.exec().execute(Arrays.copyOfRange(split, 1, split.length), null);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendClientServerInfo(Client c){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < commands.size(); i++) {
            if(commands.get(i).role().ordinal() == 0){
                sb.append(commands.get(i).command());
                if(i != commands.size() - 1){
                    sb.append(",");
                }
            }
        }

        serverSendTo(c, "commands," + sb);
        serverSendTo(c, "id," + c.ID);
    }

    public void readClientInput(String input, Client client){
        String[] inputSplit = input.split(",");

        if(inputSplit[0] == null)
            inputSplit = new String[]{""};
        String command = inputSplit[0];

        for (Command c :
                commands) {
            if(c.command().equals(command)){
                if(c.role().ordinal() == 0){
                    c.exec().execute(Arrays.copyOfRange(inputSplit, 1, inputSplit.length), client);
                    return;
                }
            }
        }

        System.out.println("From " + client.username + ":" + command);
    }

    public void serverSendTo(Client c, String message){
        c.send(serverSocket.getInetAddress().getHostAddress() + ","+ message);
    }
}
