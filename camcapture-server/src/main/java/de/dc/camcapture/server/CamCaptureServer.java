package de.dc.camcapture.server;

import de.dc.camcapture.server.threads.ServerThread;
import de.dc.camcapture.server.utils.ServerUtil;

/**
 * @author Thomas Reno
 */
public class CamCaptureServer {

	public static void main(String[] args) {
		int port = Integer.parseInt(ServerUtil.getProperty("server.port"));
		CamCaptureServer server = new CamCaptureServer();
		server.start(port);
	}

	private void start(int port) {
		Thread serverThread = new Thread(new ServerThread(port));
		serverThread.start();
	}
}
