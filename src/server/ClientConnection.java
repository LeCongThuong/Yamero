package server;

import java.net.*;
import java.io.*;

import helpers.FileHelper;
import helpers.MessageControlHelper;

public class ClientConnection {
    // client info
    private int id;
    private ServerSocket servConnection;
    private Socket controlSocket;
    private Socket[] dataSockets;
    private int port;
    private String ip;
    private static final int bufferSize = 1024;
    private static final int nThreads = 2; //TODO: add to config file
    private static String forwarderIp = null;

    // input
    private InputStream inputStream = null;
    private DataInputStream dataInputStream = null;

    // output
    private OutputStream outputStream = null;
    private DataOutputStream dataOutputStream = null;

    public ClientConnection(int clientId, ServerSocket connection) {
        this.id = clientId;
        this.servConnection = connection;
        try {
            controlSocket = connection.accept();
            dataSockets = new Socket[nThreads];
        } catch (IOException e) {
            System.out.println("Accept client connection error!");
            e.printStackTrace();
        }
        ip = controlSocket.getInetAddress().toString().replace("/", "");
        port = controlSocket.getPort();
        try {
            // Input init
            inputStream = controlSocket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);

            // Output init
            outputStream = controlSocket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);

            // the first client connect to server will be forwarder
            if (forwarderIp == null) {
                forwarderIp = ip;
            }
            // Send forwarderIp back to client
            MessageControlHelper.sendForwarderNotify(dataOutputStream, forwarderIp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        try {
            dataOutputStream.writeUTF(message);
        } catch (Exception e) {
            System.out.println("Can not send message to client");
        }
    }

    public boolean isSuccess() throws IOException {
        return dataInputStream.readBoolean();
    }

    public void sendFile(String fileName) throws SocketException {
        try {
            // TODO: modify path when done testing
            String filePath = "server/" + fileName;
            File file = new File(filePath);
            long fileSize = file.length();

            // TODO: add # of parts(threads) to FileInfo
            MessageControlHelper.sendFileInfo(dataOutputStream, new MessageControlHelper.FileInfo(fileName, fileSize));

            // TODO: splitFile() => paths  (make temp dir)
            String[] chunkPaths = FileHelper.splitFile(filePath, nThreads);
            System.out.println("DEBUG: split file done");

            Thread[] dataThreads = new Thread[nThreads];
            for (int threadIndex = 0; threadIndex < nThreads; threadIndex++) {
                dataSockets[threadIndex] = this.servConnection.accept();
                DataOutputStream parallelOutputStream = new DataOutputStream(dataSockets[threadIndex].getOutputStream());
                int finalThreadIndex = threadIndex;
                dataThreads[threadIndex] = new Thread(() -> {
                    try {
                        FileHelper.sendFile(parallelOutputStream, chunkPaths[finalThreadIndex]);
                    } catch (IOException e) {
                        System.out.println("Error in file sending threads");
                        e.printStackTrace();
                    }
                });
                dataThreads[threadIndex].start();
                // TODO: merge parts of files (path, nthreads)

            }

            for (Thread thread: dataThreads)
                thread.join();

            FileHelper.removeTempChunks(chunkPaths);

        } catch (IOException | InterruptedException e) {
            System.out.println("Something went wrong when send file");
        }
    }

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
