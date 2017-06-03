package de.dc.camcapture.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

/**
 * @author Thomas Reno
 */
public class CamCaptureServer {

	private class SocketServerThread implements Runnable {

		@Override
		public void run() {
			System.out.println("running socket server thread...");
			File file = new File("test.jpg");
			try ( //
				ServerSocket serverSocket = new ServerSocket(port); //
				Socket clientSocket = serverSocket.accept(); //
				DataInputStream dis = new DataInputStream(clientSocket.getInputStream()); //
				ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream()); //
				PrintWriter pw = new PrintWriter(clientSocket.getOutputStream()) //
			) {
				pw.println("CamCapture-Server version 0.0.1-SNAPSHOT");
				System.out.println("available: " + dis.available());
				buffer = new byte[dis.available()];
				System.out.println("buffer: " + buffer.toString());
				String input;
				while (null != (input = dis.readUTF())) {
					System.out.println("Connection " + ++count + " from server " + clientSocket.getInetAddress() + ":"
							+ clientSocket.getPort());
					System.out.println("---> " + input);
					InputStreamReader isr = new InputStreamReader(System.in);
					BufferedReader br = new BufferedReader(isr);
					String yes = br.readLine();
					if ("y".equals(yes) || "j".equals(yes)) {
						try (FileInputStream fis = new FileInputStream(file)) {
							byte[] buffer = new byte[fis.available()];
							fis.read(buffer);
							oos.writeObject(buffer);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();

			}
		}

		private byte[] buffer;

		private int count = 0;
	}

	public static void main(String[] args) {
		System.out.println("starting http tunnel server...");
		try (InputStream is = CamCaptureServer.class.getClassLoader().getResourceAsStream("server.properties")) {
			Properties props = new Properties();
			props.load(is);
			int port = Integer.parseInt(props.getProperty("server.port"));
			CamCaptureServer server = new CamCaptureServer(port);
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private final int port;

	public CamCaptureServer(int port) {
		this.port = port;
	}

	private void start() {
		Thread serverThread = new Thread(new SocketServerThread());
		serverThread.start();
	}
}
