package helpers;

import java.io.*;
import java.util.*;

public class FileHelper {
    private static int defaultBufferSize = 1024;
    private static String tempDirPath = "server/tmp/";

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
                for (QueueThread forwarderThread : forwarderThreads) {
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

    public static String[] splitFile(String filePath, int nChunks) {
        String[] chunkPaths = new String[nChunks];
        //TODO: move to property section
        int bufSize = 1024;
        byte[] buffer = new byte[bufSize];

        File originFile = new File(filePath);
        try {
            FileInputStream fio = new FileInputStream(originFile);
            int chunkSize = (int) originFile.length() / nChunks;

            for (int chunkIndex = 0; chunkIndex < nChunks; chunkIndex++) {
                //TODO: read and write to chunk
                chunkPaths[chunkIndex] = tempDirPath + originFile.getName() + chunkIndex;
                FileOutputStream chunkos = new FileOutputStream(new File(chunkPaths[chunkIndex]));
                int bytesToRead = chunkSize;
                while (bytesToRead > 0) {
                    int readLength = Math.min(bytesToRead, bufSize);
                    int nBytesRead = fio.read(buffer, 0, readLength);
                    bytesToRead = bytesToRead - nBytesRead;
                    chunkos.write(buffer);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return chunkPaths;
    }

    public static void removeTempChunks(String[] chunkPaths) {
        for (String chunkPath : chunkPaths) {
            File chunk = new File(chunkPath);
            if (chunk.delete()) {
                System.out.println("Deleted: " + chunkPath);
            } else {
                System.out.println("Fail to delete: " + chunkPath);
            }
        }
    }

    public static void mergeFileAndClearChunks(String filePathBase, int nChunks) throws IOException {
        FileOutputStream finalFileOutputStream = new FileOutputStream(new File(filePathBase));
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        String[] chunkPaths = new String[nChunks];

        // read through chunk and append to final file
        for (int chunkIndex = 0; chunkIndex < nChunks; chunkIndex++) {
            chunkPaths[chunkIndex] = filePathBase + chunkIndex;
            File chunk = new File(chunkPaths[chunkIndex]);
            FileInputStream chunkFileInputStream = new FileInputStream(chunk);
            int totalBytesRead = 0;
            int chunkSize = (int) chunk.length();
            while (totalBytesRead < chunkSize) {
                int nBytes = chunkFileInputStream.read(buffer);
                finalFileOutputStream.write(buffer, 0, nBytes);
                totalBytesRead += nBytes;
            }
        }

        // delete chunk after merged
        removeTempChunks(chunkPaths);

        finalFileOutputStream.close();
    }
}
