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

    public static int sendFile(OutputStream outputStream, String filepath) throws IOException {
        int bufferSize = loadBufferSize();
        System.out.println("FileHelper sending file: " + filepath + " with bufferSize: " + bufferSize);
        BufferedWriter outSocket = new BufferedWriter(new OutputStreamWriter(outputStream));
        try {
            File file = new File(filepath);
            BufferedReader fileInputStream = new BufferedReader(new FileReader(file));
            char[] buffer = new char[bufferSize];
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

    public static int receiveFile(InputStream inputStream, String filepath, long fileSize) throws IOException {
        int bufferSize = loadBufferSize();
        System.out.println("FileHelper receiving file: " + filepath + " with bufferSize: " + bufferSize);

        BufferedReader inpSocket = new BufferedReader(new InputStreamReader(inputStream));

        try {
            BufferedWriter fileOutputStream = new BufferedWriter(new FileWriter(filepath));
            char[] buffer = new char[bufferSize];
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

    public static int forwardFile(InputStream inputStream, ArrayList<OutputStream> forwardingStreams, String filepath, long fileSize) throws IOException {
        int bufferSize = loadBufferSize();
        System.out.println("FileHelper receiving and forwarding file: " + filepath + " with bufferSize: " + bufferSize);
        // create threads to forward file through forwardingSockets
        ArrayList<QueueThread> forwarderThreads = new ArrayList<>();
        BufferedWriter fileOutputStream;

        BufferedReader inpSocket = new BufferedReader(new InputStreamReader(inputStream));

        try {
            fileOutputStream = new BufferedWriter(new FileWriter(filepath));
            for (OutputStream forwardingStream: forwardingStreams) {
                BufferedWriter forwardingSocket = new BufferedWriter(new OutputStreamWriter(forwardingStream));
                QueueThread forwarder = new QueueThread() {
                    @Override
                    public void onQueue() {
                        char[] bytes = (char[]) this.getData();
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
            char[] buffer = new char[bufferSize];
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
            for (QueueThread forwarderThread: forwarderThreads) {
                forwarderThread.kill();
            }
        }

        return 0;
    }
}
