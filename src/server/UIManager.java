package server;

import java.util.Scanner;

public class UIManager {
    private static Scanner scanner = new Scanner(System.in);

    public void serverStart(int port) {
        System.out.println("Server is listening at port: " + port);
        System.out.println("C1: Waiting...");
        System.out.println("C2: Waiting...");
        System.out.println("C3: Waiting...");
    }

    public void clientConnected(ClientConnection c) {
        int clientId = c.getId();
        System.out.print("\033[" + (4 - clientId) + "AC" + clientId + ": " + c.getIp() + ":" + c.getPort() + " Connected.\033[" + (4 - clientId) + "B\r");
    }

    public String getFileName() {
        System.out.print("Enter file name: ");
        return scanner.nextLine().trim();
    }

    public void fileNotFound(String fileName) {
        System.out.println("Cannot find file '" + fileName + "'!\n");
    }

    public void sendFileSuccess(long time) {
        System.out.println("Send file finish in " + time + " ms\n");
    }
}
