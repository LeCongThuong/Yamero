package server;

import java.net.*;
import java.io.*;
import helpers.FileHelper;
import helpers.MessageControlHelper;

public class ClientConnection {
    // client info
    private int id;
    private Socket socket;
    private int port;
    private String ip;
    private static final int bufferSize = 1024;

    // input
    private InputStream inputStream = null;
    private DataInputStream dataInputStream = null;

    // output
    private OutputStream outputStream = null;
    private DataOutputStream dataOutputStream = null;

    public ClientConnection(int clientId, Socket connection) {
        id = clientId;
        socket = connection;
        ip = socket.getInetAddress().toString().replace("/", "");
        port = socket.getPort();
        try {
            // Input init
            inputStream = socket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);

            // Output init
            outputStream = socket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);

            // Send clientId back to client
            dataOutputStream.write(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TODO: handle error
    public void sendMessage(String message) {
        try {
            dataOutputStream.writeUTF(message);
        } catch (Exception e) {
            System.out.println("Can not send message to client");
        }
    }

    // TODO: handle error
    public boolean isSuccess() throws IOException {
        return dataInputStream.readBoolean();
    }

    public void sendFile(String fileName) {
        try {
            File file = new File(fileName);
            long fileSize = file.length();
            MessageControlHelper.sendFileInfo(dataOutputStream, new MessageControlHelper.FileInfo(fileName, fileSize));
            FileHelper.sendFile(dataOutputStream, fileName);
        } catch (IOException e) {
            System.out.println("Something went wrong when send file");
        }
    }

    // TODO: handle error
    public long getFinishTime() throws IOException {
        return dataInputStream.readLong();
    }

    public int getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }
}
