package client;

import helpers.ConfigLoader;
import helpers.FileHelper;
import helpers.MessageControlHelper;
import helpers.MessageControlHelper.*;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.ConnectException;

public class Controller {
    // client info
    private static int id;
    private static ClientAddress[] clientAddress = new ClientAddress[3];

    // server info
    private static final int serverPort = 9090;
    private static final int nThreads = 2; //TODO: move to config, rename

    // input utils
    private static InputStream inputStream = null;
    private static DataInputStream dataInputStream = null;

    // output utils
    private static OutputStream outputStream = null;
    private static DataOutputStream clientOutputStream = null;


    public static void main(String[] args) {
        try {
            //connect to server
            Socket socket = connectionHandle(getServerIp(), serverPort);
            // Input init
            inputStream = socket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);

            // Output init
            outputStream = socket.getOutputStream();
            clientOutputStream = new DataOutputStream(outputStream);

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
                executeAsForwarderClient();
            } else {
                executeAsNormalClient();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getServerIp() {
        final String localhost = "127.0.0.1";
        try {
            ConfigLoader configLoader = new ConfigLoader("client/config.properties");
            if (configLoader.getProperty("server-ip") != null) {
                return configLoader.getProperty("server-ip");
            }
            return localhost;
        } catch (Exception e) {
            return localhost;
        }
    }

    private static void executeAsForwarderClient() throws IOException {
        // c1 wait for c2 and c3 connect
        ServerSocket serverControlSocket = new ServerSocket(9000);
        clientOutputStream.writeBoolean(true);
        System.out.println("Listening at port 9000");

        while (true) {
            // forward file info
            FileInfo fileInfo = MessageControlHelper.receiveFileInfo(dataInputStream);
            if (fileInfo != null) {
                System.out.println("Forwarder: " + fileInfo.fileName + " " + fileInfo.fileSize);
            } else {
                System.out.println("Error receiving file info");
                return;
            }


            // connect c2
            Socket c2ControlSocket = serverControlSocket.accept();
            System.out.println("C2(" + clientAddress[1].getIp() + ":" + clientAddress[1].getPort() + ") " + "connected");
            DataOutputStream c2ControlOutputStream = new DataOutputStream(c2ControlSocket.getOutputStream());

            // connect c3
            Socket c3ControlSocket = serverControlSocket.accept();
            System.out.println("C3(" + clientAddress[2].getIp() + ":" + clientAddress[2].getPort() + ") " + " connected");
            DataOutputStream c3ControlOutputStream = new DataOutputStream(c3ControlSocket.getOutputStream());

            MessageControlHelper.sendFileInfo(c2ControlOutputStream, fileInfo);
            MessageControlHelper.sendFileInfo(c3ControlOutputStream, fileInfo);

            Socket[] forwarderDataSockets = new Socket[nThreads];
            DataInputStream[] forwarderParallelInputStreams = new DataInputStream[nThreads];

            Socket[] c2DataSockets = new Socket[nThreads];
            Socket[] c3DataSockets = new Socket[nThreads];
            DataOutputStream[] c3DataOutputStreams = new DataOutputStream[nThreads];
            DataOutputStream[] c2DataOutputStreams = new DataOutputStream[nThreads];
            for (int threadIndex = 0; threadIndex < nThreads; threadIndex++) {

                forwarderDataSockets[threadIndex] = connectionHandle(getServerIp(), serverPort);
                forwarderParallelInputStreams[threadIndex] = new DataInputStream(forwarderDataSockets[threadIndex].getInputStream());

                c2DataSockets[threadIndex] = serverControlSocket.accept();
                System.out.println("Client 2 thread " + threadIndex + " connected");
                c3DataSockets[threadIndex] = serverControlSocket.accept();
                System.out.println("Client 3 thread " + threadIndex + " connected");
                c3DataOutputStreams[threadIndex] = new DataOutputStream(c3DataSockets[threadIndex].getOutputStream());
                c2DataOutputStreams[threadIndex] = new DataOutputStream(c2DataSockets[threadIndex].getOutputStream());

                System.out.println("DEBUG: " + "prepare to init thread");

                int finalThreadIndex = threadIndex;
                int finalThreadIndex1 = threadIndex;
                new Thread(() -> {
                    try {
                        System.out.println("Forwarder started thread " + finalThreadIndex);
                        FileHelper.forwardFile(forwarderParallelInputStreams[finalThreadIndex],
                                new ArrayList<>(Arrays.asList(c2DataOutputStreams[finalThreadIndex1], c2DataOutputStreams[finalThreadIndex1])),
                                "./c1/" + fileInfo.fileName,
                                fileInfo.fileSize);
                        long finishTime = System.currentTimeMillis();
                        clientOutputStream.writeLong(finishTime);
                        System.out.println("Receive file " + fileInfo.fileName + " successfully.");
                    } catch (IOException e) {
                        System.out.println("Error in file sending threads");
                        e.printStackTrace();
                    }
                });

            }

            // Receive and Forward file
        }
    }

    private static void executeAsNormalClient() throws IOException, InterruptedException {

        Socket forwarderControlSocket = connectionHandle(clientAddress[0].getIp(), 9000);
        clientOutputStream.writeBoolean(true);
        System.out.println("Connected to C1 at " + clientAddress[0].getIp() + ":9000");

        DataInputStream c1ControlInputStream = new DataInputStream(forwarderControlSocket.getInputStream());
        Socket[] normalDataSockets = new Socket[2];
        DataInputStream[] normalDataInputStreams = new DataInputStream[nThreads];

        for (int socketIndex = 0; socketIndex < nThreads; socketIndex++) {
            normalDataSockets[socketIndex] = connectionHandle(clientAddress[0].getIp(), 9000);

            // TODO: temporary - delay two connection establishment two threads
            normalDataInputStreams[socketIndex] = new DataInputStream(normalDataSockets[socketIndex].getInputStream());
            Thread.sleep(2000);
        }

        while (true) {
            FileInfo fileInfo = MessageControlHelper.receiveFileInfo(c1ControlInputStream);
            if (fileInfo != null) {
                System.out.println("Receiver " + fileInfo.fileName + " " + fileInfo.fileSize);
            } else {
                System.out.println("Error getting file info");
                return;
            }
            String filepath = "./c" + id + "/" + fileInfo.fileName;
            for (int threadIndex = 0; threadIndex < nThreads; threadIndex++) {
                int finalThreadIndex = threadIndex;
                new Thread(() -> {
                    try {
                        System.out.println("Normal client started thread " + finalThreadIndex);
                        FileHelper.receiveFile(normalDataInputStreams[finalThreadIndex], filepath, fileInfo.fileSize);
                        long finishTime = System.currentTimeMillis();
                        System.out.println("Receive file " + fileInfo.fileName + " successfully.");
                        clientOutputStream.writeLong(finishTime);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
//            FileHelper.receiveFile(c1ControlInputStream, filepath, fileInfo.fileSize);
        }
    }

    private static Socket connectionHandle(String targetIp, int port) throws IOException {
        boolean isSuccessConnection = false;
        while (!isSuccessConnection) {
            try {
                Socket socket = new Socket(targetIp, port);
                isSuccessConnection = true;
                System.out.println(socket.toString());
                return socket;
            } catch (ConnectException e) {
                System.out.println("Connect failed, waiting and trying again");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
        return null;
    }
}
