package de.dc.camcapture.server;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Properties;

/**
 * @author Thomas Reno
 */
public class CamCaptureServer {

	private class SocketServerThread implements Runnable {

		@Override
		public void run() {
			System.out.println("running socket server thread...");
			try ( //
				ServerSocket serverSocket = new ServerSocket(port); //
				Socket clientSocket = serverSocket.accept(); //
				DataInputStream dis = new DataInputStream(clientSocket.getInputStream()); //
				ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream())
			) {
				String address = clientSocket.getInetAddress().toString();
				int port = clientSocket.getPort();
				System.out.println("Connection " + ++count + " with client " + address + ":" + port);
				String input;
				while (null != (input = dis.readUTF())) {
					System.out.println("--> " + input);
					WatchService watcher = FileSystems.getDefault().newWatchService();
					String dir = PROPS.getProperty("watcher.dir");
					Path path = Paths.get(dir);
					WatchKey key = path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
					WATCH_LOOP: while (true) {
						for (WatchEvent<?> event : key.pollEvents()) {
							System.out.println(event.kind());
							if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
								// Waiting 1 sec till file is finished written
								sleep(1000L);
								String filename = ((Path) event.context()).toFile().getName();
								File file = new File(dir, filename);
								System.out.println(file);
								try (FileInputStream fis = new FileInputStream(file)) {
									byte[] buffer = new byte[fis.available()];
									System.out.println("buffer size: " + buffer.length);
									fis.read(buffer);
									oos.writeObject(buffer);
								}
								break WATCH_LOOP;
							}
						}
					}
				}
			} catch (IOException e) {
				System.err.println("Connection failed");
				e.printStackTrace();
			}
		}

		private int count = 0;
	}

	private static final Properties PROPS = new Properties();

	static {
		try (InputStream is = CamCaptureServer.class.getClassLoader().getResourceAsStream("server.properties")) {
			PROPS.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		System.out.println("starting cam-capture-server...");
		int port = Integer.parseInt(PROPS.getProperty("server.port"));
		CamCaptureServer server = new CamCaptureServer(port);
		server.start();
	}

	private final int port;

	public CamCaptureServer(int port) {
		this.port = port;
	}

	private void start() {
		Thread serverThread = new Thread(new SocketServerThread());
		serverThread.start();
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// nothing to do...
		}
	}
}
