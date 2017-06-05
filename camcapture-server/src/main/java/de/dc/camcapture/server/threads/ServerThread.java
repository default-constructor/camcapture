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
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dc.camcapture.server.utils.ServerUtil;
import de.dc.camcapture.server.utils.Tuple;

/**
 * @author Thomas Reno
 */
public class ServerThread implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(ServerThread.class);

	@Override
	public void run() {
		try ( //
			ServerSocket serverSocket = new ServerSocket(port); //
			Socket clientSocket = serverSocket.accept(); //
			DataInputStream dis = new DataInputStream(clientSocket.getInputStream()); //
			ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream()) //
		) {
			String address = clientSocket.getInetAddress().getHostAddress();
			LOG.info("Connected to {}", address);
			String defaultPath = System.getProperty("user.home") + "/CamCapture/Pictures";
			String path = System.getenv("CAMCAPTURE_PICTURES");
			String dir = null != path ? path : defaultPath;
			WatchKey key = Paths.get(dir).register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
			String input;
			while (null != (input = dis.readUTF())) {
				ServerUtil.validateToken(address, input);
				LOG.info("Verified token from {}", address);
				Tuple<String, File> fileTuple = watchIncomingFile(dir, key);
				String filename = fileTuple.left;
				File file = fileTuple.right;
				String string = filename.substring(0, filename.indexOf("."));
				LocalDateTime dateTime = ServerUtil.getDateTime(string, "yyyyMMdd_HHmmss");
				long timestamp = ServerUtil.getMillis(dateTime);
				try (FileInputStream fis = new FileInputStream(file)) {
					byte[] buffer = new byte[fis.available()];
					fis.read(buffer);
					oos.writeLong(timestamp);
					oos.writeUTF(filename);
					oos.writeObject(buffer);
					LOG.info("Pushed image {} to {}", filename, address);
				}
			}
		} catch (IOException | IllegalArgumentException e) {
			LOG.error(e.getMessage(), e);
			// Waiting 10 sec so the server can cool down
			ServerUtil.sleep(10000L);
			run();
		}
	}

	private final WatchService watcher;

	private final int port;

	public ServerThread(int port) {
		this.port = port;
		try {
			watcher = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Tuple<String, File> watchIncomingFile(String dir, WatchKey key) {
		while (true) {
			for (WatchEvent<?> event : key.pollEvents()) {
				if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
					// Waiting 1 sec so the file can be finished written
					ServerUtil.sleep(1000L);
					String filename = ((Path) event.context()).toFile().getName();
					File file = new File(dir, filename);
					LOG.info("Detected file {}", filename);
					return new Tuple<String, File>(filename, file);
				}
			}
		}
	}
}
