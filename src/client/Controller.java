package client;

import java.net.*;
import java.io.*;
import java.util.Properties;

public class Controller {
    // client info
    private static int id;
    private static ClientAddress[] clientAddress = new ClientAddress[3];
    private static final int bufferSize = 1024;

    // server info
    private static final int serverPort = 9090;
    private static final String serverIp = "127.0.0.1";

    // input utils
    private static InputStream inputStream = null;
    private static DataInputStream dataInputStream = null;

    // output utils
    private static OutputStream outputStream = null;
    private static DataOutputStream dataOutputStream = null;

    public static void main(String[] args) {
        try {
            InetAddress serverAddress = InetAddress.getByName(getServerIp());
            System.out.println("SERVER IP: " + serverAddress);
            Socket socket = new Socket(serverAddress, serverPort);

            // Input init
            inputStream = socket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);

            // Output init
            outputStream = socket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);

            // identify client
            id = dataInputStream.readByte();
            System.out.println("C" + id + " connected to server.");

            // get other client info
            String message = dataInputStream.readUTF();
            String[] address = message.split(" ", 3);
            for (int i = 0; i < 3; i++) {
                clientAddress[i] = new ClientAddress(address[i]);
            }

            if (id == 1) {
                // c1 wait for c2 and c3 connect
                ServerSocket serverSocket = new ServerSocket(9000);
                dataOutputStream.writeBoolean(true);
                System.out.println("Listening at port 9000");

                // connect c2
                Socket c2 = serverSocket.accept();
                System.out.println("C2(" + clientAddress[1].getIp() + ":" + clientAddress[1].getPort() + ") connected");
                DataOutputStream c2OutputStream = new DataOutputStream(c2.getOutputStream());

                // connect c3
                Socket c3 = serverSocket.accept();
                System.out.println("C3(" + clientAddress[2].getIp() + ":" + clientAddress[2].getPort() + ") connected");
                DataOutputStream c3OutputStream = new DataOutputStream(c3.getOutputStream());

                while (true) {
                    // forward file name
                    String fileName = dataInputStream.readUTF();
                    c2OutputStream.writeUTF(fileName);
                    c3OutputStream.writeUTF(fileName);
                    // forward file size
                    long fileSize = dataInputStream.readLong();
                    c2OutputStream.writeLong(fileSize);
                    c3OutputStream.writeLong(fileSize);
                    // Receive file
                    FileOutputStream fileOutputStream = new FileOutputStream("./c1/" + fileName);
                    byte[] buffer = new byte[bufferSize];
                    long totalBytesRead = 0;
                    while (totalBytesRead < fileSize) {
                        int nBytes = dataInputStream.read(buffer);
                        fileOutputStream.write(buffer, 0, nBytes);
                        c2OutputStream.write(buffer, 0, nBytes);
                        c3OutputStream.write(buffer, 0, nBytes);
                        totalBytesRead += nBytes;
                    }
                    long finishTime = System.currentTimeMillis();
                    dataOutputStream.writeLong(finishTime);
                    System.out.println("Receive file " + fileName + " successfully.");
                    fileOutputStream.close();
                }
            } else {
                Socket c1 = new Socket(clientAddress[0].getIp(), 9000);
                dataOutputStream.writeBoolean(true);
                System.out.println("Connected to C1 at " + clientAddress[0].getIp() + ":9000");
                DataInputStream c1InputStream = new DataInputStream(c1.getInputStream());
                while (true) {
                    // receive file name
                    String fileName = c1InputStream.readUTF();
                    // receive file size
                    long fileSize = c1InputStream.readLong();
                    // Receive file
                    FileOutputStream fileOutputStream = new FileOutputStream("./c" + id + "/" + fileName);
                    byte[] buffer = new byte[bufferSize];
                    long totalBytesRead = 0;
                    while (totalBytesRead < fileSize) {
                        int nBytes = c1InputStream.read(buffer);
                        fileOutputStream.write(buffer, 0, nBytes);
                        totalBytesRead += nBytes;
                    }
                    long finishTime = System.currentTimeMillis();
                    dataOutputStream.writeLong(finishTime);
                    System.out.println("Receive file " + fileName + " successfully.");
                    fileOutputStream.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getServerIp() {
        try {
            Properties properties = new Properties();
            InputStream inputStream = new FileInputStream("client/config.properties");
            properties.load(inputStream);
            return properties.getProperty("server-ip");
        } catch (IOException e) {
            e.printStackTrace();
            return "127.0.0.1";
        }
    }
}
