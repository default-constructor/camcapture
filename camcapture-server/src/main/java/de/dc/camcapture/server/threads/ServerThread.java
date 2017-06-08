package de.dc.camcapture.server.threads;

import static de.dc.camcapture.server.utils.ServerUtil.*;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dc.camcapture.model.Snapshot;
import de.dc.camcapture.server.threads.WatcherThread.Listener;

/**
 * @author Thomas Reno
 */
public class ServerThread implements Runnable, Listener {

	private static final Logger LOG = LoggerFactory.getLogger(ServerThread.class);

	private static final String CONNECTIVITY_CHECK = "check";

	private static final String FILE_TRANSFER = "transfer";

	@Override
	public void run() {
		LOG.debug("running server thread");
		String address = "";
		try ( //
			ServerSocket serverSocket = new ServerSocket(port); //
			Socket clientSocket = serverSocket.accept(); //
			DataInputStream dis = new DataInputStream(clientSocket.getInputStream()); //
			ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream()) //
		) {
			address = clientSocket.getInetAddress().getHostAddress();
			LOG.info("Connected -> {}", address);
			String input;
			while (null != (input = dis.readUTF())) {
				validateToken(address, input);
				LOG.info("Verified connection -> {}", address);
				if (null == snapshot) {
					// Connectivity check in 5 seconds
					sleep(5000L);
					oos.writeUTF(CONNECTIVITY_CHECK);
				} else {
					oos.writeUTF(FILE_TRANSFER);
					LOG.debug("created at {}", snapshot.getCreatedAt());
					oos.writeObject(snapshot);
					LOG.info("Pushed snapshot -> {} {}", snapshot.getFilename(), address);
					// Clearing file to enable connectivity check again
					snapshot = null;
				}
				oos.flush();
			}
		} catch (EOFException e) {
			LOG.error("Connection lost -> {}", address);
		} catch (IOException | IllegalArgumentException e) {
			LOG.error(e.getMessage(), e);
		}
		// Re-running server in 10 seconds
		sleep(10000L);
		run();
	}

	@Override
	public void onSnapshotDetected(Snapshot snapshot) {
		this.snapshot = snapshot;
	}

	private Snapshot snapshot;

	private final int port;

	public ServerThread(int port) {
		this.port = port;
	}
}
