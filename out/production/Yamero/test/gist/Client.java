import java.io.*;
import java.net.*;

public class Client {

	public static void main(String[] args) throws Exception {
		Socket socket = new Socket("192.168.2.2", 6666);
		BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
		long total = 0;
		long start = System.currentTimeMillis();

		byte[] bytes = new byte[10240]; // 10K
		while (true) {
			int read = input.read(bytes);
			total += read;
			long cost = System.currentTimeMillis() - start;
			if (cost > 0 && System.currentTimeMillis() % 10 == 0) {
				System.out.println("Read " + total + " bytes, speed: " + total / cost + "KB/s");
			}
		}
	}

}