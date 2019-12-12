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
    private static String forwarderIp;

    // server info
    private static final int serverPort = 9090;
    private static int nThreads = 2; //TODO: move to config, rename

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

            // receive forwarder ip
            forwarderIp = MessageControlHelper.receiveForwarderNotify(dataInputStream);

            if (isThisMyIpAddress(forwarderIp)) {
                executeAsForwarderClient();
            } else {
                executeAsNormalClient();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isThisMyIpAddress(String ip) throws UnknownHostException {
        InetAddress addr = InetAddress.getByName(ip);
        // Check if the address is a valid special local or loop back
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
            return true;

        // Check if the address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (SocketException e) {
            return false;
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
        ServerSocket forwarderControlSocket = new ServerSocket(9000);
        System.out.println("Listening at port 9000");

        // connect c2
        Socket c2ControlSocket = forwarderControlSocket.accept();
        ClientAddress c2Address = new ClientAddress(c2ControlSocket);
        System.out.println("C2(" + c2Address.getIp() + ":" + c2Address.getPort() + ") " + "connected");
        DataOutputStream c2ControlOutputStream = new DataOutputStream(c2ControlSocket.getOutputStream());

        // connect c3
        Socket c3ControlSocket = forwarderControlSocket.accept();
        ClientAddress c3Address = new ClientAddress(c3ControlSocket);
        System.out.println("C3(" + c3Address.getIp() + ":" + c3Address.getPort() + ") " + "connected");
        DataOutputStream c3ControlOutputStream = new DataOutputStream(c3ControlSocket.getOutputStream());

        clientOutputStream.writeBoolean(true);

        while (true) {
            // forward file info
            FileInfo fileInfo = MessageControlHelper.receiveFileInfo(dataInputStream);
            System.out.println("DEBUG: receive file info");
            if (fileInfo != null) {
                System.out.println("Forwarder: " + fileInfo.fileName + " " + fileInfo.fileSize);
                nThreads = fileInfo.nChunks;
                System.out.println("DEBUG: number of threads: " + nThreads);
            } else {
                System.out.println("Error receiving file info");
                return;
            }

            MessageControlHelper.sendFileInfo(c2ControlOutputStream, fileInfo);
            MessageControlHelper.sendFileInfo(c3ControlOutputStream, fileInfo);

            Socket[] forwarderDataSockets = new Socket[nThreads];
            DataInputStream[] forwarderDataInputStreams = new DataInputStream[nThreads];

            Socket[] c2DataSockets = new Socket[nThreads];
            Socket[] c3DataSockets = new Socket[nThreads];
            DataOutputStream[] c3DataOutputStreams = new DataOutputStream[nThreads];
            DataOutputStream[] c2DataOutputStreams = new DataOutputStream[nThreads];

            int normalChunkSize = (int) (fileInfo.fileSize / nThreads);
            int lastChunkSize = normalChunkSize + (int) fileInfo.fileSize % nThreads;
            String filePathBase = "client/" + fileInfo.fileName;
            Thread[] forwardDataThreads = new Thread[nThreads];
            for (int threadIndex = 0; threadIndex < nThreads; threadIndex++) {

                forwarderDataSockets[threadIndex] = connectionHandle(getServerIp(), serverPort);
                forwarderDataInputStreams[threadIndex] = new DataInputStream(forwarderDataSockets[threadIndex].getInputStream());

                c2DataSockets[threadIndex] = forwarderControlSocket.accept();
                System.out.println("Client 2 thread " + threadIndex + " connected - port: " + c2DataSockets[threadIndex].getPort());
                c3DataSockets[threadIndex] = forwarderControlSocket.accept();
                System.out.println("Client 3 thread " + threadIndex + " connected - port: " + c3DataSockets[threadIndex].getPort());
                c3DataOutputStreams[threadIndex] = new DataOutputStream(c3DataSockets[threadIndex].getOutputStream());
                c2DataOutputStreams[threadIndex] = new DataOutputStream(c2DataSockets[threadIndex].getOutputStream());

                int chunkSize = (threadIndex == nThreads - 1 && lastChunkSize != 0) ? lastChunkSize : normalChunkSize;

                int finalThreadIndex = threadIndex;
                forwardDataThreads[threadIndex] = new Thread(() -> {
                    try {
                        System.out.println("Forwarder started thread " + finalThreadIndex);
                        // TODO: modify file size when update protocol
                        FileHelper.forwardFile(forwarderDataInputStreams[finalThreadIndex],
                                new ArrayList<>(Arrays.asList(c2DataOutputStreams[finalThreadIndex], c3DataOutputStreams[finalThreadIndex])),
                                filePathBase + finalThreadIndex,
                                chunkSize);
                        System.out.println("Receive file " + fileInfo.fileName + " successfully.");
                    } catch (IOException e) {
                        System.out.println("Error in file sending threads");
                        e.printStackTrace();
                    }
                });

                forwardDataThreads[threadIndex].start();
            }

            // wait for all data thread to join, notify server time that complete sending file to forwarder
            try {
                for (Thread forwardDataThread : forwardDataThreads) {
                    forwardDataThread.join();
                }
                System.out.println("DEBUG: Forwarder data threads completed");

                // merge parts of received file
                FileHelper.mergeFileAndClearChunks(filePathBase, nThreads);

                long finishTime = System.currentTimeMillis();
                System.out.println("DEBUG: Finish at: " + finishTime);
                clientOutputStream.writeLong(finishTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void executeAsNormalClient() throws IOException, InterruptedException {

        Socket forwarderControlSocket = connectionHandle(forwarderIp, 9000);
        ClientAddress forwarderAddress = new ClientAddress(forwarderControlSocket);
        DataInputStream c1ControlInputStream = new DataInputStream(forwarderControlSocket.getInputStream());

        clientOutputStream.writeBoolean(true);
        System.out.println("Connected to C1 at " + forwarderAddress.getIp() + ":9000");


        // file transmission loop
        while (true) {
            FileInfo fileInfo = MessageControlHelper.receiveFileInfo(c1ControlInputStream);
            if (fileInfo != null) {
                System.out.println("Receiver " + fileInfo.fileName + " " + fileInfo.fileSize);
                nThreads = fileInfo.nChunks;
            } else {
                System.out.println("Error getting file info");
                return;
            }

            Socket[] normalDataSockets = new Socket[nThreads];
            DataInputStream[] normalDataInputStreams = new DataInputStream[nThreads];

            for (int socketIndex = 0; socketIndex < nThreads; socketIndex++) {
                normalDataSockets[socketIndex] = connectionHandle(forwarderAddress.getIp(), 9000);
                // TODO: temporary - delay two connection establishment two threads
                normalDataInputStreams[socketIndex] = new DataInputStream(normalDataSockets[socketIndex].getInputStream());
                Thread.sleep(15);
            }

            int normalChunkSize = (int) (fileInfo.fileSize / nThreads);
            int lastChunkSize = normalChunkSize + (int) fileInfo.fileSize % nThreads;

            String filePathBase = "client/" + fileInfo.fileName;
            Thread[] normalClientDataThreads = new Thread[nThreads];
            for (int threadIndex = 0; threadIndex < nThreads; threadIndex++) {
                int finalThreadIndex = threadIndex;
                int chunkSize = (threadIndex == nThreads - 1 && lastChunkSize != 0) ? lastChunkSize : normalChunkSize;
                normalClientDataThreads[threadIndex] = new Thread(() -> {
                    try {
                        System.out.println("Normal client started thread " + finalThreadIndex);
                        // TODO: modify file size when update protocol
                        FileHelper.receiveFile(normalDataInputStreams[finalThreadIndex],
                                filePathBase + finalThreadIndex,
                                chunkSize);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                normalClientDataThreads[threadIndex].start();
            }

            for (Thread normalClientDataThread : normalClientDataThreads)
                normalClientDataThread.join();

            // merge parts of files (path, nthreads)
            FileHelper.mergeFileAndClearChunks(filePathBase, nThreads);

            long finishTime = System.currentTimeMillis();
            System.out.println("Receive file " + fileInfo.fileName + " successfully.");
//            System.out.println("DEBUG: Finish at: " + finishTime);
            clientOutputStream.writeLong(finishTime);
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
