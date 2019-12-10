package helpers;

import java.io.*;
import java.util.*;

public class FileHelper {
    private static int defaultBufferSize = 1024;

    private static int loadBufferSize() {
        try {
            ConfigLoader configLoader = new ConfigLoader("helpers/config.properties");
            String bufferSizeConf = configLoader.getProperty("file-buffer-size");
            if (bufferSizeConf == null) return defaultBufferSize;
            return Integer.parseInt(bufferSizeConf);
        } catch (Exception e) {
            return defaultBufferSize;
        }
    }

    public static int sendFile(DataOutputStream outSocket, String filepath) throws IOException {
        int bufferSize = loadBufferSize();
        System.out.println("FileHelper sending file: " + filepath + " with bufferSize: " + bufferSize);
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
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    public static int receiveFile(DataInputStream inpSocket, String filepath, long fileSize) throws IOException {
        int bufferSize = loadBufferSize();
        System.out.println("FileHelper receiving file: " + filepath + " with bufferSize: " + bufferSize);
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
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    public static int forwardFile(DataInputStream inpSocket, ArrayList<DataOutputStream> forwardingSockets, String filepath, long fileSize) throws IOException {
        int bufferSize = loadBufferSize();
        System.out.println("FileHelper receiving and forwarding file: " + filepath + " with bufferSize: " + bufferSize);
        // create threads to forward file through forwardingSockets
        ArrayList<QueueThread> forwarderThreads = new ArrayList<>();
        FileOutputStream fileOutputStream;

        try {
            fileOutputStream = new FileOutputStream(filepath);
            for (DataOutputStream forwardingSocket : forwardingSockets) {
                QueueThread forwarder = new QueueThread() {
                    @Override
                    public void onQueue() {
                        byte[] bytes = (byte[]) this.getData();
                        try {
                            forwardingSocket.write(bytes, 0, bytes.length);
                        } catch (IOException e) {
                            e.printStackTrace();
                            this.kill(true);
                        }
                    }
                };
                forwarderThreads.add(forwarder);
                forwarder.start();
            }
            byte[] buffer = new byte[bufferSize];
            long totalBytesRead = 0;

            while (totalBytesRead < fileSize) {
                int nBytes = inpSocket.read(buffer);
                fileOutputStream.write(buffer, 0, nBytes);
                totalBytesRead += nBytes;
                for (QueueThread forwarderThread: forwarderThreads) {
                    forwarderThread.pushData(Arrays.copyOf(buffer, nBytes));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return -1;
        } finally {
            for (QueueThread forwarderThread : forwarderThreads) {
                forwarderThread.kill();
            }
        }

        return 0;
    }
}
