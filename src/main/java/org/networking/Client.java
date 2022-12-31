package org.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
    public Socket connectedSocket;
    public Server connectedServer;
    public DataInputStream in;
    public DataOutputStream out;
    public String username = "unknown client";
    public int ID = 0;
    public boolean isServerProp = false;
    public boolean strictReceive = false;

    public Client(Socket socket, String username) {
        this.username = username;
        try {
            this.connectedSocket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            sendClientInfo();
            Thread readThread = new Thread(this::read);
            readThread.start();

            Thread writeThread = new Thread(this::userInput);
            writeThread.start();
        }
        catch (Exception e){
            System.out.println(e);
        }
    }

    private void sendClientInfo() {
        try {
            send("setUsername,"+username);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void userInput() {
        while (true){
            if(isServerProp)
                break;
            Scanner scanner = new Scanner(System.in);
            String message = scanner.nextLine();
            try {
                // Send message to server

                if(message.equals("disconnect")) {
                    try {
                        connectedSocket.close();
                    }
                    catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    break;
                }

                send(message);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void read(){
        while (true) {
            try {
                String command = in.readUTF();
                infoProcessing(command);

            } catch (Exception e) {
                System.out.println("Error: " + e);
                break;
            }
        }
        System.out.println("Client " + username + " disconnected");
    }

    public void infoProcessing(String input){
        if(isServerProp) {
            connectedServer.readClientInput(input, this);
            return;
        }

        String[] splitInput = input.split(",");

        if(splitInput[0].equals(""))
            splitInput = new String[]{""};

        String command = splitInput[0];

        String[] args = new String[]{};
        if(splitInput.length > 1) {
            args = new String[splitInput.length - 1];
            System.arraycopy(splitInput, 1, args, 0, splitInput.length - 1);
        }
        boolean trusted = false;

        if(connectedServer == null)
        {
            connectedServer = new Server();
        }
        if(args[0] == null)
            args = new String[]{""};


        if(command.equals(connectedSocket.getInetAddress().getHostAddress()) || !strictReceive) {
            if (args[0].equals("commands")) {
                connectedServer.commands = new ArrayList<>();
                System.out.println("Received Commands:");
                for (int i = 1; i < args.length; i++) {
                    connectedServer.commands.add(new Command(Role.CLIENT, args[i]));
                    System.out.print(args[i] + "\n");
                }
            } else if (args[0].equals("id")) {
                System.out.println("My ID is now: " + args[1]);
                ID = Integer.parseInt(args[1]);
            }
            else{
                System.out.println("Received: " + args[0]);
            }
        }
    }

    public void send(String message){
        try {
            out.writeUTF(message);
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }
}
