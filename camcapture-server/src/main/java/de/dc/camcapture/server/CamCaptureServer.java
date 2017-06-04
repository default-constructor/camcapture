package de.dc.camcapture.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dc.camcapture.server.threads.ServerThread;
import de.dc.camcapture.server.utils.ServerUtil;

/**
 * @author Thomas Reno
 */
public class CamCaptureServer {

	private static final Logger LOG = LoggerFactory.getLogger(CamCaptureServer.class);

	public static void main(String[] args) {
		int port = Integer.parseInt(ServerUtil.getProperty("server.port"));
		CamCaptureServer server = new CamCaptureServer();
		server.start(port);
	}

	private void start(int port) {
		LOG.info("Starting [{}]", CamCaptureServer.class.getSimpleName());
		Thread serverThread = new Thread(new ServerThread(port));
		serverThread.start();
	}
}
