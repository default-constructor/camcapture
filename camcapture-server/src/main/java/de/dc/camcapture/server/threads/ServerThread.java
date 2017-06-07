package de.dc.camcapture.server.threads;

import static de.dc.camcapture.server.utils.ServerUtil.*;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dc.camcapture.server.model.PictureFile;
import de.dc.camcapture.server.threads.WatcherThread.Listener;
import de.dc.camcapture.server.utils.ServerUtil;

/**
 * @author Thomas Reno
 */
public class ServerThread implements Runnable, Listener {

	private static final Logger LOG = LoggerFactory.getLogger(ServerThread.class);

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
				ServerUtil.validateToken(address, input);
				LOG.info("Verified connection -> {}", address);
				if (null == pictureFile) {
					// Connectivity check every 10 seconds
					sleep(10000L);
					oos.writeLong(-1L); // -1 = Connectivity check
				} else {
					long timestamp = pictureFile.getCreatedAt();
					String filename = pictureFile.getFilename();
					try (FileInputStream fis = new FileInputStream(pictureFile)) {
						byte[] buffer = new byte[fis.available()];
						fis.read(buffer);
						oos.writeLong(timestamp);
						oos.writeUTF(filename);
						oos.writeObject(buffer);
						LOG.info("Pushed picture -> {} {}", filename, address);
					}
					// File clearing to enable connectivity check again
					pictureFile = null;
				}
				oos.flush();
			}
		} catch (EOFException e) {
			LOG.error("Lost Connection to {}", address);
		} catch (IOException | IllegalArgumentException e) {
			LOG.error(e.getMessage(), e);
		}
		// Waiting 10 seconds to cool down the server
		sleep(10000L);
		run();
	}

	@Override
	public void onPictureDetected(PictureFile pictureFile) {
		this.pictureFile = pictureFile;
	}

	private PictureFile pictureFile;

	private final int port;

	public ServerThread(int port) {
		this.port = port;
	}
}
