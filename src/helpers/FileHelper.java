package helpers;

import java.io.*;
import java.util.*;

public class FileHelper {
    private static int bufferSize = 1024;

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

    synchronized public static void setBufferSize(int bufferSize) {
        FileHelper.bufferSize = bufferSize;
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
            // create threads to forward file through forwardingSockets
            ArrayList<Forwarder> forwarderThreads = new ArrayList<>();
            for (DataOutputStream forwardingSocket: forwardingSockets) {
                Forwarder forwarder = new Forwarder(forwardingSocket);
                forwarderThreads.add(forwarder);
                forwarder.start();
            }

            while (totalBytesRead < fileSize) {
                int nBytes = inpSocket.read(buffer);
                fileOutputStream.write(buffer, 0, nBytes);
                totalBytesRead += nBytes;
                for (Forwarder forwarderThread: forwarderThreads) {
//                    forwardingSocket.write(buffer, 0, nBytes);
                    forwarderThread.pushData(Arrays.copyOf(buffer, nBytes));
                }
            }
            fileOutputStream.close();
            for (Forwarder forwarderThread: forwarderThreads) {
                forwarderThread.turnoff();
            }
        } catch (FileNotFoundException e) {
            return -1;
        }
        return 0;
    }

    static class Forwarder extends Thread {
        public Boolean stop = false;
        public Queue<byte[]> mQueue = new LinkedList<>();
        private DataOutputStream outSocket;

        Forwarder(DataOutputStream outSocket) {
            this.outSocket = outSocket;
        }

        synchronized public void turnoff() {
            this.stop = true;
        }

        synchronized private boolean dataQueueIsEmpty() {
            return mQueue.size() == 0;
        }

        synchronized public void pushData(byte[] bytes) {
            mQueue.add(bytes);
        }

        synchronized public byte[] getData() {
            return mQueue.remove();
        }

        @Override
        public void run() {
            while (!stop || !this.dataQueueIsEmpty()) {
                if (!this.dataQueueIsEmpty()) {
                    byte[] bytes = this.getData();
                    try {
                        this.outSocket.write(bytes, 0, bytes.length);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
