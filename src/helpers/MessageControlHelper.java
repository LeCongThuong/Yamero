package helpers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

public class MessageControlHelper {
    public static final String FILE_INFO = "FILE_INFO";
    public static final String FORWARDER_NOTIFY = "FORWARDER_NOTIFY";

    private static ArrayList<String> parseMessage(byte[] bytes) {
        return new ArrayList<>();
    }

    public static class FileInfo {
        public final String fileName;
        public final long fileSize;

        public FileInfo(String fileName, long fileSize) {
            this.fileName = fileName;
            this.fileSize = fileSize;
        }
    }

    public static void sendFileInfo(DataOutputStream outSocket, FileInfo fileInfo) throws IOException {
       outSocket.writeUTF(fileInfo.fileName);
       outSocket.writeLong(fileInfo.fileSize);
    }

    public static FileInfo receiveFileInfo(DataInputStream inpSocket) throws IOException {
        String fileName= inpSocket.readUTF();
        long fileSize = inpSocket.readLong();
        return new FileInfo(fileName, fileSize);
    }

    public static void sendForwarderNotify(DataOutputStream outSocket, String forwarderIp) throws IOException {
        try {
            byte[] addressInBytes = InetAddress.getByName(forwarderIp).getAddress();
            outSocket.write(addressInBytes, 0, addressInBytes.length);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static String receiveForwarderNotify(DataInputStream inpSocket) throws IOException {
        byte[] addressInBytes = new byte[64];
        int nBytes = inpSocket.read(addressInBytes);
        addressInBytes = Arrays.copyOf(addressInBytes, nBytes);
        String forwarderIp = InetAddress.getByAddress(addressInBytes).toString().replace("/", "");
        return forwarderIp;
    }
}
