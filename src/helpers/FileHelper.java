package helpers;

import java.io.*;
import java.util.ArrayList;

public class FileHelper {
    private static int bufferSize = 1024;

    synchronized public static void setBufferSize(int bufferSize) {
        FileHelper.bufferSize = bufferSize;
    }

    public static int sendFile(DataOutputStream outSocket, String filepath) throws IOException {
        try {
            File file = new File(filepath);
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[bufferSize];
            int nBytes;
            while ((nBytes = fileInputStream.read(buffer)) != -1) {
                outSocket.write(buffer, 0, nBytes);
            }
            outSocket.flush();
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            return -1;
        }
        return 0;
    }

    public static int receiveFile(DataInputStream inpSocket, String filepath, long fileSize) throws IOException {
        System.out.println("FileHelper sending file: " + filepath);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filepath);
            byte[] buffer = new byte[bufferSize];
            long totalBytesRead = 0;
            while (totalBytesRead < fileSize) {
                int nBytes = inpSocket.read(buffer);
                fileOutputStream.write(buffer, 0, nBytes);
                totalBytesRead += nBytes;
            }
        } catch (FileNotFoundException e) {
            return -1;
        }
        return 0;
    }

    public static int forwardFile(DataInputStream inpSocket, ArrayList<DataOutputStream> forwardingSockets, String filepath, long fileSize) throws IOException {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filepath);
            byte[] buffer = new byte[bufferSize];
            long totalBytesRead = 0;
            while (totalBytesRead < fileSize) {
                int nBytes = inpSocket.read(buffer);
                fileOutputStream.write(buffer, 0, nBytes);
                totalBytesRead += nBytes;
                for (DataOutputStream forwardingSocket: forwardingSockets) {
                    forwardingSocket.write(buffer, 0, nBytes);
                }
            }
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            return -1;
        }
        return 0;
    }
}
