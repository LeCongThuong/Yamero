package client;

import java.net.*;

public class ClientAddress {
    private int port;
    private String ip;

    public ClientAddress(Socket socket) {
        ip = socket.getInetAddress().toString().replace("/", "");
        port = socket.getPort();
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}