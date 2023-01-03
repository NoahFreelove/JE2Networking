package JE.Networking.Server;

import java.util.ArrayList;

public class ServerConfig {
    public boolean whitelist = false;
    public ArrayList<String> whitelistIPs = new ArrayList<>();
    public ArrayList<String> bannedIPs = new ArrayList<>();
    public String ip = "localhost";
    public int port = 400;
}
