package test.bandwidth;

import helpers.ConfigLoader;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class client {
    public static void main(String[] args) {
        ConfigLoader configLoader = new ConfigLoader("test/bandwidth/config.properties");
        String serverIP = configLoader.getProperty("server-ip") != null
                ? configLoader.getProperty("server-ip")
                : "192.168.0.1";
        int port = Integer.parseInt(configLoader.getProperty("port"));
        try {
            Socket connSocket = new Socket(serverIP, port);
            DataInputStream dataInputStream = new DataInputStream(connSocket.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(connSocket.getOutputStream());

            byte[] bytes = new byte[1024];

            while (true) {
                int testSize = dataInputStream.readInt();
                dataOutputStream.writeBoolean(true);
                int nBytes;
                long readBytes = 0;
                while (readBytes < testSize) {
                    nBytes = dataInputStream.read(bytes);
                    readBytes += nBytes;
                }
                System.out.println("Read: " + (float) readBytes/1024 + " kB.");
                dataOutputStream.writeBoolean(true);
//                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
