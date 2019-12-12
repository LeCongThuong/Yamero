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

            int nThreads = 2;
            // wait for 3 client
            ClientConnection c1 = new ClientConnection(1, serverSocket, nThreads);
            uiManager.clientConnected(c1);
            ClientConnection c2 = new ClientConnection(2, serverSocket, nThreads);
            uiManager.clientConnected(c2);
            ClientConnection c3 = new ClientConnection(3, serverSocket, nThreads);
            uiManager.clientConnected(c3);

            // check if all client is ready to receive file
            c1.isSuccess();

//            System.out.println("DEBUG: all connected");

            // ready to send file
            while (true) {
                String fileName = uiManager.getFileName();
                // TODO: remove when done testing

                File file = new File("server/" + fileName);
                if (file.exists() && file.isFile()) {
                    long startTime = System.currentTimeMillis();
//                    System.out.println("DEBUG: start sending at: " + startTime);
                    // TODO: add to config file
                    c1.sendFile(fileName);
                    long c1_time = c1.getFinishTime();
                    long c2_time = c2.getFinishTime();
                    long c3_time = c3.getFinishTime();
                    long finishTime = Math.max(c1_time, Math.max(c2_time, c3_time));

                    uiManager.displayMessageInline("C1 Response time: ");
                    uiManager.sendFileSuccess(c1_time - startTime);
                    System.out.println("DEBUG: C3 finish at " + c1_time);

                    uiManager.displayMessageInline("C2 Response time: ");
                    uiManager.sendFileSuccess(c2_time - startTime);
                    System.out.println("DEBUG: C2 finish at " + c2_time);

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
            uiManager.displayMessageInline("\nSomething went wrong. Restart project and try again");
        }
    }
}
