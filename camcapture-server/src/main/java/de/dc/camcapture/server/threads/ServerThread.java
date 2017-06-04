package de.dc.camcapture.server.threads;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dc.camcapture.server.utils.ServerUtil;

/**
 * @author Thomas Reno
 */
public class ServerThread implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(ServerThread.class);

	@Override
	public void run() {
		LOG.info("Started [{}] [{}]", ServerThread.class.getSimpleName(), port);
		try ( //
				ServerSocket serverSocket = new ServerSocket(port); //
				Socket clientSocket = serverSocket.accept(); //
				DataInputStream dis = new DataInputStream(clientSocket.getInputStream()); //
				ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream()) //
		) {
			String clientAddress = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
			LOG.info("Connected [{}] [{}]", clientAddress, ++count);
			WatchService watcher = FileSystems.getDefault().newWatchService();
			String dir = ServerUtil.getProperty("watcher.dir");
			Path path = Paths.get(dir);
			WatchKey key = path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
			String input;
			while (null != (input = dis.readUTF())) {
				if (!ServerUtil.checkToken(input)) {
					LOG.error("Access denied [{}]", clientAddress);
					throw new IllegalArgumentException("Invalid token");
				}
				LOG.info("Verified [{}]", clientAddress);
				WATCH_LOOP: while (true) {
					for (WatchEvent<?> event : key.pollEvents()) {
						if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
							String filename = ((Path) event.context()).toFile().getName();
							File file = new File(dir, filename);
							LOG.info("Detected [{}]", file);
							// Waiting 1 sec so the file can be finished written
							ServerUtil.sleep(1000L);
							try (FileInputStream fis = new FileInputStream(file)) {
								oos.writeObject(filename.getBytes());
								byte[] buffer = new byte[fis.available()];
								fis.read(buffer);
								oos.writeObject(buffer);
								LOG.info("Pushed [{}] [{}]", filename, clientAddress);
							}
							break WATCH_LOOP;
						}
					}
				}
			}
		} catch (IOException | IllegalArgumentException e) {
			LOG.error("An error has occured [{}]", e.getMessage(), e);
			// Waiting 10 sec so the server can cool down
			ServerUtil.sleep(10000L);
			run();
		}
	}

	private int count = 0;

	private final int port;

	public ServerThread(int port) {
		this.port = port;
	}
}
