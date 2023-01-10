package JE.Networking.Server;

import JE.Networking.Client;
import JE.Networking.Commands.Command;
import JE.Networking.Commands.CommandExec;
import JE.Networking.Events.DisconnectReason;
import JE.Networking.Commands.Role;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    protected transient ServerConfig config = new ServerConfig();

    public Server(){}

    public Server(Command... defaultCommands){
        this.commands = new ArrayList<>(Arrays.asList(defaultCommands));
    }

    volatile CopyOnWriteArrayList<Client> clients = new CopyOnWriteArrayList<>();

    public ArrayList<Command> commands = new ArrayList<>(Arrays.stream(new Command[]{
            new Command(Role.CLIENT, "disconnectFromServer", "none", (args, initiatedBy) -> {
                disconnectClient(initiatedBy, DisconnectReason.CLIENT_DISCONNECTED);
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
                    serverSendTo(client, "commands;" + args[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            })),
            new Command(Role.CLIENT, "whisper", "[Arg1 - client index], [Arg2 - message]", (args, initiatedBy) -> {
                if(Integer.parseInt(args[0]) > clients.size())
                    return;
                serverSendTo(clients.get(Integer.parseInt(args[0])), "whisper from '" + initiatedBy.username + "':\n" + args[1]);
            }),
            new Command(Role.SERVER, "ban", "[Arg1 - client index]", new CommandExec() {
                @Override
                public void execute(String[] args, Client initiatedBy) {
                    banClient(clients.get(Integer.parseInt(args[0])));
                }
            }),
            new Command(Role.SERVER, "unban", "[Arg1 - client IP]", (args, initiatedBy) -> unbanIP(args[0])),
            new Command(Role.SERVER, "kick", "[Arg1 - client index]", (args, initiatedBy) -> disconnectClient(clients.get(Integer.parseInt(args[0])), DisconnectReason.KICKED)),
            new Command(Role.CLIENT, "STARTPACKET", "", (args, initiatedBy) -> {
                initiatedBy.isInPacket = true;
            }),


    }).toList());

    ServerSocket serverSocket;

    public void open() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName(config.ip), config.port));

            Thread inputThread = new Thread(this::serverInput);
            inputThread.start();

            Thread queue = new Thread(() -> {
                while (true) {
                    try {
                        Socket s = serverSocket.accept();
                        if(config.bannedIPs.contains(s.getInetAddress().getHostAddress()) || (config.whitelist && !config.whitelistIPs.contains(s.getInetAddress().getHostAddress()))) {
                            try {
                                s.close();
                            }catch (Exception ignore){}
                            return;
                        }

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

            System.out.println("Now accepting incoming connections on port: " + config.port + "; " + serverSocket.getInetAddress().getHostAddress());
            System.out.println("---Commands---");
            commands.forEach(command -> System.out.println(command.command() + " : " + command.role()));
            System.out.println("---End Commands---");

            queue.start();
        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
    }

    private void serverInput() {
        while (true){

            Scanner scanner = new Scanner(System.in);
            String message = scanner.nextLine();
            String[] split = message.split(";");
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

    private void sendClientServerInfo(Client c){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < commands.size(); i++) {
            if(commands.get(i).role().ordinal() == 0){
                sb.append(commands.get(i).command());
                if(i != commands.size() - 1){
                    sb.append(";");
                }
            }
        }
        keyGen(c);
        serverSendTo(c, "KEY;" + c.key);

        serverSendTo(c, "commands;" + sb);
        serverSendTo(c, "id;" + c.ID);
        sendClientGameInfo(c);
        c.connected = true;
    }
    // Override for sending your game data. map, players, etc.
    protected void sendClientGameInfo(Client c){}

    // Override for any keygen method you want
    protected void keyGen(Client c){
        Random rand = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(rand.nextInt(10));
        }
        c.key = sb.toString();
    }

    public void readClientInput(String input, Client client){
        String[] inputSplit = input.split(";");

        if(inputSplit[0] == null)
            inputSplit = new String[]{""};
        //System.out.println("From " + client.username + ":" + Arrays.toString(inputSplit));

        String command = inputSplit[0];

        if(client.isInPacket){
            if(command.equals("ENDPACKET")){
                sendObjectToClients(client, client.packetBuffer);
                client.isInPacket = false;
                return;
            }
            client.packetAppend(command,Arrays.copyOfRange(inputSplit,1,inputSplit.length));
        }

        boolean found = false;
        for (Command c :
                commands) {
            if(c.command().equals(command)){
                if(c.role().ordinal() == 0){
                    c.exec().execute(Arrays.copyOfRange(inputSplit, 1, inputSplit.length), client);
                    found = true;
                    break;
                }
            }
        }
        if(found)
            return;
    }

    public void serverSendTo(Client c, String message){
        c.send(c.key + ";"+ message);
    }
    public void serverSendTo(Client c, String command, String... args){
        StringBuilder sb = new StringBuilder();
        sb.append(command);
        for (String arg :
                args) {
            sb.append(";").append(arg);
        }
        c.send(c.key + ";"+ sb);
    }

    public void sendObjectToClients(Client ignore, byte[] bytes){
        for (Client c :
                clients) {
            if(c != ignore){
            }
            PacketManager.sendPacket(c.key + ";",Arrays.toString(bytes),bytes.length,65535,c.out,"OBJUPDATE");
        }
    }

    public void disconnectClient(Client c, DisconnectReason reason)
    {
        if(!c.connectedSocket.isClosed()){

            serverSendTo(c,"DISCONNECT;" + reason.ordinal());
            // make sure the client knows they are disconnected
            try {
                c.connectedSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        c.connected = false;
        clients.remove(c);
    }

    // Only use this if you know what you are doing
    public void softDisconnect(Client c){
        c.connected = false;
        clients.remove(c);
    }

    public void banClient(Client c){
        config.bannedIPs.add(c.connectedSocket.getInetAddress().getHostAddress());
        disconnectClient(c, DisconnectReason.BANNED);
    }
    public void unbanIP(String ip){
        config.bannedIPs.remove(ip);
    }
}
