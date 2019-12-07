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
    private static String forwarderIp = null;

    // server info
    private static final int serverPort = 9090;

    // input utils
    private static InputStream inputStream = null;
    private static DataInputStream dataInputStream = null;

    // output utils
    private static OutputStream outputStream = null;
    private static DataOutputStream dataOutputStream = null;

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

    public static void main(String[] args) {
        try {
            //connect to server
            Socket socket = connectionHandle(getServerIp(), serverPort);
            // Input init
            inputStream = socket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);

            // Output init
            outputStream = socket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);

            // identify client
            forwarderIp = MessageControlHelper.receiveForwarderNotify(dataInputStream);
            System.out.println("Connected to server.");

            if (isThisMyIpAddress(forwarderIp)) {
                executeAsForwarderClient();
            } else {
                executeAsNormalClient();
            }
        } catch (Exception e) {
            System.out.println("Connection error, please make sure your config is correct then try again.\n");
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
        ServerSocket serverSocket = new ServerSocket(9000);
        System.out.println("Listening at port 9000");

        // connect c2
        Socket c2 = serverSocket.accept();
        ClientAddress c2Address = new ClientAddress(c2);
        System.out.println("C2(" + c2Address.getIp() + ":" + c2Address.getPort() + ") connected");
        DataOutputStream c2OutputStream = new DataOutputStream(c2.getOutputStream());

        // connect c3
        Socket c3 = serverSocket.accept();
        ClientAddress c3Address = new ClientAddress(c3);
        System.out.println("C3(" + c3Address.getIp() + ":" + c3Address.getPort() + ") connected");
        DataOutputStream c3OutputStream = new DataOutputStream(c3.getOutputStream());

        // notify Server ready to send file
        dataOutputStream.writeBoolean(true);

        while (true) {
            // forward file info
            FileInfo fileInfo = MessageControlHelper.receiveFileInfo(dataInputStream);
            System.out.println("Forwarder: " + fileInfo.fileName + " " + fileInfo.fileSize);
            MessageControlHelper.sendFileInfo(c2OutputStream, fileInfo);
            MessageControlHelper.sendFileInfo(c3OutputStream, fileInfo);

            // Receive and Forward file
            String filePath = "./" + fileInfo.fileName;
            try {
                FileHelper.forwardFile(dataInputStream, new ArrayList<>(Arrays.asList(c2OutputStream, c3OutputStream)), filePath, fileInfo.fileSize);
            } catch (FileNotFoundException e) {
                System.out.println("Something went wrong, please make sure this file path available: " + filePath);
                dataOutputStream.writeLong(-1);
                continue;
            }
            long finishTime = System.currentTimeMillis();
            dataOutputStream.writeLong(finishTime);
            System.out.println("Receive file " + fileInfo.fileName + " successfully.");
        }
    }

    private static void executeAsNormalClient() throws IOException {
        // connect to c1
        Socket c1 = connectionHandle(forwarderIp, 9000);
        ClientAddress c1Address = new ClientAddress(c1);
        System.out.println("Connected to C1 at " + c1Address.getIp() + ":9000");
        DataInputStream c1InputStream = new DataInputStream(c1.getInputStream());

        while (true) {
            // receive file when forwarded
            FileInfo fileInfo = MessageControlHelper.receiveFileInfo(c1InputStream);
            System.out.println("Receiver " + fileInfo.fileName + fileInfo.fileSize);
            String filePath = "./" + fileInfo.fileName;
            try {
                FileHelper.receiveFile(c1InputStream, filePath, fileInfo.fileSize);
            } catch (FileNotFoundException e) {
                System.out.println("Something went wrong, please make sure this file path available: " + filePath);
                dataOutputStream.writeLong(-1);
                continue;
            }
            long finishTime = System.currentTimeMillis();
            dataOutputStream.writeLong(finishTime);
            System.out.println("Receive file " + fileInfo.fileName + " successfully.");
        }
    }

    private static Socket connectionHandle(String targetIp, int port) throws IOException {
        boolean isSuccessConnection = false;
        while(!isSuccessConnection){
            try{
                Socket socket = new Socket(targetIp, port);
                isSuccessConnection = true;
                System.out.println(socket.toString());
                return socket;
            }
            catch (ConnectException e){
                System.out.println("Connect failed, waiting and trying again");
                try{
                    Thread.sleep(1000);
                }
                catch(InterruptedException ie){
                    ie.printStackTrace();
                }
            }
        }
        return null;
    }
}
