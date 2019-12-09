package test.bandwidth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import helpers.ConfigLoader;

public class server {
    public static void main(String[] args) {
        ConfigLoader configLoader = new ConfigLoader("test/bandwidth/config.properties");
        int port = Integer.parseInt(configLoader.getProperty("port"));
        int bufferSize = Integer.parseInt(configLoader.getProperty("buffer-size"));
        Scanner scanner = new Scanner(System.in);

        try {
            ServerSocket socket = new ServerSocket(port);
            System.out.println("Server is listening on port: " + port + "...");
            Socket connSocket = socket.accept();
            System.out.println("Connected from: " + connSocket.getRemoteSocketAddress());

            DataInputStream dataInputStream = new DataInputStream(connSocket.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(connSocket.getOutputStream());

            while (true) {
                System.out.print("FileSize to test (in kB): ");
                int testSize = scanner.nextInt();
                testSize *= 1024;
                byte[] bytes = new byte[bufferSize];
                boolean flag;

                dataOutputStream.writeInt(testSize);
                flag = dataInputStream.readBoolean();
                if (!flag) { continue; }

                long startSending = System.currentTimeMillis();

                long sentBytes = 0;
                while (sentBytes < testSize) {
                    dataOutputStream.write(bytes, 0, Math.min(bufferSize, (int) (testSize - sentBytes)));
                    sentBytes += Math.min(bufferSize, testSize - sentBytes);
                }

                flag = dataInputStream.readBoolean();
                if (flag) {
                    long endSending = System.currentTimeMillis();
                    long executeTime = endSending - startSending;
                    System.out.println("Sending " + testSize + "kB in " + executeTime + " ms.");
                    System.out.println("Estimated bandwith: " + (float) (testSize / (executeTime / 1000.0)) + "kB/s");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
