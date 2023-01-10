package JE.Networking.Server;

import JE.Networking.Client;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PacketManager {

    static private List<byte[]> splitIntoPackets(String str, int arrSize, int packetSize) {
        List<byte[]> packets = new ArrayList<>();
        byte[] data = Client.stringToByteArray(arrSize,str);
        int numPackets = (int) Math.ceil((double) data.length / packetSize);
        for (int i = 0; i < numPackets; i++) {
            int start = i * packetSize;
            int end = Math.min(start + packetSize, data.length);
            byte[] packet = Arrays.copyOfRange(data, start, end);
            packets.add(packet);
        }
        return packets;
    }

    /**
     * Send packets to client
     * @param append What to append to the start of each packet (usually the server/client key)
     * @param data The large string/data to send
     * @param dataSize The length of the data (in bytes)
     * @param maxSize The max size in bytes of each packet (TCP has a max packet size of 65535 bytes)
     * @param out The data output stream to send the packets to
     * @param end Command to send after the packets have all been sent
     */
    static public void sendPacket(String append, String data, int dataSize, int maxSize, DataOutputStream out, String end){
        List<byte[]> packets = splitIntoPackets(data,dataSize, maxSize);
        try {
            out.writeUTF(append + "STARTPACKET");
        }catch (Exception e){
            System.out.println("Error starting packet");
        }
        for (byte[] packet : packets) {
            try {
                out.writeUTF(append +packet.length + ";" + Arrays.toString(packet));
            }catch (Exception e){
                System.out.println("Error sending packet");
            }
        }
        try {
            out.writeUTF(append + "ENDPACKET");
            if(!end.equals("")){
                out.writeUTF(append + end);
            }
        }catch (Exception e){
            System.out.println("Error ending packet");
        }
    }
}
