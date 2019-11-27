package client;

public class ClientAddress {
    private int port;
    private String ip;

    public ClientAddress(String addr) {
        String[] args = addr.split(":", 2);
        ip = args[0];
        port = Integer.parseInt(args[1]);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}