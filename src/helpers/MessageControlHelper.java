package helpers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;

public class MessageControlHelper {
    public static final String FILE_INFO = "FILE_INFO";
    public static final String FORWARDER_NOTIFY = "FORWARDER_NOTIFY";

    private static ArrayList<String> parseMessage(byte[] bytes) {
        return new ArrayList<>();
    }

    public static void sendFileInfo(DataOutputStream outSocket, String fileName, String fileSize) {

    }

    public static void receiveFileInfo(DataInputStream inpSocket) {

    }

    public static void sendForwarderNotify(DataOutputStream outSocket) {

    }

    public static void receiveForwarderNotify(DataInputStream inpSocket) {

    }
}
