import java.io.*;
import java.net.*;

public class Server {
  public static void main(String[] args) throws Exception {
    ServerSocket server = new ServerSocket(6666);
    Socket socket = server.accept();
    BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());

    byte[] bytes = new byte[10 * 1024]; // 10K
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = 12;
    }

    while (true) {
      output.write(bytes);
    }
  }
}