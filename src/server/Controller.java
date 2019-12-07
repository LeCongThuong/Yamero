package server;

import java.net.*;
import java.io.*;

public class Controller {
    private static int port = 9090;
    private static int bufferSize = 1024;

    public static void main(String[] args) {
        UIManager uiManager = new UIManager();
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            uiManager.serverStart(port);

            // wait for 3 client
            ClientConnection c1 = new ClientConnection(1, serverSocket.accept());
            uiManager.clientConnected(c1);
            ClientConnection c2 = new ClientConnection(2, serverSocket.accept());
            uiManager.clientConnected(c2);
            ClientConnection c3 = new ClientConnection(3, serverSocket.accept());
            uiManager.clientConnected(c3);

            // send all client address to every client
            String message = c1.getIp() + ":" + c1.getPort() + " " + c2.getIp() + ":" + c2.getPort() + " " + c3.getIp() + ":" + c3.getPort();
            c1.sendMessage(message);
            c1.isSuccess();
            c2.sendMessage(message);
            c2.isSuccess();
            c3.sendMessage(message);
            c3.isSuccess();

            // ready to send file
            while (true) {
                String fileName = uiManager.getFileName();
                File file = new File(fileName);
                if (file.exists() && file.isFile()) {
                    long startTime = System.currentTimeMillis();
                    c1.sendFile(fileName);
                    long c1_time = c1.getFinishTime();
                    long c2_time = c2.getFinishTime();
                    long c3_time = c3.getFinishTime();
                    long finishTime = Math.max(c1_time, Math.max(c2_time, c3_time));

                    uiManager.displayMessageInline("C1 Response time: ");
                    uiManager.sendFileSuccess(c1_time - startTime);

                    uiManager.displayMessageInline("C2 Response time: ");
                    uiManager.sendFileSuccess(c2_time - startTime);

                    uiManager.displayMessageInline("C3 Response time: ");
                    uiManager.sendFileSuccess(c3_time - startTime);

                    uiManager.displayMessageInline("Overall response time: ");
                    uiManager.sendFileSuccess(finishTime - startTime);
                } else {
                    uiManager.fileNotFound(fileName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
