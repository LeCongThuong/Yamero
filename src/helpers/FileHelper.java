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
        // create threads to forward file through forwardingSockets
        ArrayList<Forwarder> forwarderThreads = new ArrayList<>();
        FileOutputStream fileOutputStream;

        try {
            fileOutputStream = new FileOutputStream(filepath);
            for (DataOutputStream forwardingSocket: forwardingSockets) {
                Forwarder forwarder = new Forwarder(forwardingSocket);
                forwarderThreads.add(forwarder);
                forwarder.start();
            }
            byte[] buffer = new byte[bufferSize];
            long totalBytesRead = 0;

            while (totalBytesRead < fileSize) {
                int nBytes = inpSocket.read(buffer);
                fileOutputStream.write(buffer, 0, nBytes);
                totalBytesRead += nBytes;
                for (Forwarder forwarderThread: forwarderThreads) {
//                    forwardingSocket.write(buffer, 0, nBytes);
                    forwarderThread.pushData(Arrays.copyOf(buffer, nBytes));
                }
            }
        } catch (FileNotFoundException e) {
            return -1;
        } finally {
            for (Forwarder forwarderThread: forwarderThreads) {
                forwarderThread.turnoff();
            }
        }

        return 0;
    }

    static class Forwarder extends Thread {
        /**
         * outSocket:
         * mQueue: data from file pushed in to mQueue and waiting to be sent
         * stoppable: is `true` when parent thread has pushed the whole file into mQueue,
         *            so Forwarder can stop after finishing sending
         */
        private Boolean stoppable = false;
        private final Queue<byte[]> mQueue = new LinkedList<>();
        private final DataOutputStream outSocket;

        private final Object stoppableLock = new Object();

        Forwarder(DataOutputStream outSocket) {
            this.outSocket = outSocket;
        }

        private void turnoff() {
            synchronized (this.stoppableLock) {
                this.stoppable = true;
            }
        }

        private Boolean canStop() {
            synchronized (this.stoppableLock) {
                return this.stoppable;
            }
        }

        private boolean queueNotEmpty() {
            synchronized (this.mQueue) {
                return this.mQueue.size() > 0;
            }
        }

        private void pushData(byte[] bytes) {
            synchronized (this.mQueue) {
                this.mQueue.add(bytes);
            }
        }

        private byte[] getData() {
            synchronized (this.mQueue) {
                return this.mQueue.remove();
            }
        }

        @Override
        public void run() {
            while (!this.canStop() || this.queueNotEmpty()) {
                if (this.queueNotEmpty()) {
                    byte[] bytes = this.getData();
                    try {
                        this.outSocket.write(bytes, 0, bytes.length);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;  // thread will stop instantly if connection failed
                    }
                }
            }
        }
    }
}
