package org.networking;

import org.networking.Commands.Command;
import org.networking.Commands.Role;
import org.networking.Events.DisconnectEvent;
import org.networking.Events.DisconnectReason;
import org.networking.Server.Server;
import org.networking.Test.Person;

import java.io.*;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
    public Socket connectedSocket;
    public Server connectedServer;
    public DataInputStream in;
    public DataOutputStream out;
    public String username = "unknown client";
    public int ID = 0;
    public boolean isServerProp = false;
    public boolean strictReceive = true;
    public transient String key = null;
    public boolean connected;
    public boolean logReceived = false;

    protected DisconnectEvent disconnectEvent = (reason) -> {
        System.out.println("Client " + ID + " disconnected from server for reason: " + reason);
    };

    private Person person = new Person("John", "Gamer", 21);

    public Client(Socket socket, String username) {

        this.username = username;
        try {
            this.connectedSocket = socket;
            connected = true;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            sendClientInfo();
            Thread readFromServerThread = new Thread(this::readFromServer);
            readFromServerThread.start();

            Thread writeThread = new Thread(this::consoleInput);
            writeThread.start();
        }
        catch (Exception e){
            e.printStackTrace();
            onDisconnect(DisconnectReason.UNKNOWN);
        }
    }

    private void sendClientInfo() {
        try {
            send("setUsername;"+username);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void consoleInput() {
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
                else if (message.equals("send"))
                {
                    sendObject(person);
                    return;
                }

                send(message);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void readFromServer(){
        while (true) {
            try {
                String command = in.readUTF();
                messageProcessing(command);

            } catch (Exception e) {
                if(isServerProp)
                {
                    connectedServer.softDisconnect(this);
                    return;
                }
                if(connected){
                    System.out.println("Read Error: " + e);
                }
                break;
            }
        }
        if(isServerProp){
            //System.out.println("Client " + username + " disconnected");
            connectedServer.disconnectClient(this, DisconnectReason.CLIENT_DISCONNECTED);
        }
        else {
            if(connected){
                onDisconnect(DisconnectReason.SERVER_CLOSED);
            }
            else
                onDisconnect(DisconnectReason.UNKNOWN);
        }
    }

    private void messageProcessing(String input){
        if(isServerProp) {
            connectedServer.readClientInput(input, this);
            return;
        }

        String[] splitInput = input.split(";");
        if(splitInput.length < 2)
            return;

        if(splitInput[0].equals(""))
            splitInput = new String[]{""};

        String command = splitInput[1];

        String[] args = new String[splitInput.length - 1];

        boolean trusted = (splitInput[0].equals(key)) || !strictReceive;

        // remove command/key
        System.arraycopy(splitInput, 1, args, 0, splitInput.length - 1);


        // Messages from the server have the first command being its key - remove command
        if(trusted || key==null){
            System.arraycopy(args, 1, args, 0, args.length-1);
        }

        if(connectedServer == null)
        {
            connectedServer = new Server();
        }

        if(args[0] == null)
            args = new String[]{""};

        if(splitInput[0].equals(key))
            trusted = true;
        if(logReceived)
            System.out.println(Arrays.toString(splitInput));

        // check if primitive command as they take priority
        if(trusted || !strictReceive || key==null)
            primitiveMessageProcessing(command,args,trusted);

        processCommand(command,args,trusted);
    }

    // override this with your own commands
    protected void processCommand(String command, String[] args, boolean trusted) {
    }

    private void primitiveMessageProcessing(String command, String[] args, boolean trusted) {

        if(key == null){
            if(command.equals("KEY")){
                key = args[0];
                System.out.println("Set key to:" + key);
            }
            return;
        }
        if(!trusted)
            return;
        switch (command) {
            case "commands" -> {
                connectedServer.commands = new ArrayList<>();
                System.out.println("---Commands---");
                for (String arg : args) {
                    connectedServer.commands.add(new Command(Role.CLIENT, arg, "", (args1, initiatedBy) -> {
                    }));
                    System.out.print(arg + "\n");
                }
                System.out.println("---End Commands---");
            }

            case "id" -> {
                System.out.println("My ID is: " + args[0]);
                ID = Integer.parseInt(args[0]);
            }
            case "DISCONNECT"->{
                onDisconnect(DisconnectReason.values()[Integer.parseInt(args[0])]);
            }
            case "OBJUPDATE" -> {
                onObjectProcessed(processObject(args[0], args[1], args[2]));
            }

            default -> System.out.println(args[0]);
        }
    }

    protected void onObjectProcessed(Object processedObject){
        System.out.println(processedObject.toString());
    }

    public void send(String message){
        try {
            out.writeUTF(message);
        } catch (Exception e) {
            if(isServerProp)
            {
                connectedServer.softDisconnect(this);
                return;
            }
            if(connected){
                System.out.println("Send Error: " + e);
            }

        }
    }


    public void sendObject(Object object){
       byte[] bytes = serialize(object);
       send("OBJSEND;" + bytes.length + ";" + object.getClass().getName() + ";" + Arrays.toString(bytes));

    }

    private Object processObject(String numBytes, String className, String byteArr){
        try {
            int byteLength = Integer.parseInt(numBytes);
            byte[] bytes = new byte[byteLength];

            // Clean incoming bytes
            String byteString = byteArr;
            byteString = byteString.replace("[", "");
            byteString = byteString.replace("]", "");
            byteString = byteString.replace(",", "");

            String[] byteStringSplit = byteString.split(" ");
            for (int i = 0; i < byteStringSplit.length; i++) {
                bytes[i] = Byte.parseByte(byteStringSplit[i]);
            }

            // Read to object
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream is = new ObjectInputStream(in);
            return is.readObject();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Object();
    }

    public byte[] serialize(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);

            // Make sure class implements NetworkingObject
            if(!(obj instanceof NetworkingObject))
                return null;

            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {

                if (field.isAnnotationPresent(SyncVar.class) ) {
                    System.out.println("Syncing field: " + field.getName());
                    field.setAccessible(true);
                    oos.writeObject(field.get(obj));
                }
            }
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void onDisconnect(DisconnectReason reason){
        if(!connected)
            return;
        connected = false;
        disconnectEvent.onDisconnect(reason);
    }
}
