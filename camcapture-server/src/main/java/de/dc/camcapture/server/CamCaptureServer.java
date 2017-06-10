package de.dc.camcapture.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dc.camcapture.server.threads.ServerThread;
import de.dc.camcapture.server.threads.WatcherThread;
import de.dc.camcapture.server.utils.ServerUtil;

/**
 * @author Thomas Reno
 */
public class CamCaptureServer {

	private static final Logger LOG = LoggerFactory.getLogger(CamCaptureServer.class);

	private static final String VARIABLE_USERHOME = "user.home";
	private static final String VARIABLE_PICTURES = "camcapture.pictures";

	private static final String KEY_SERVERPORT = "server.port";

	private static final String PATH_PICTURES = "/CamCapture/Pictures";
	private static final String PATH_PICTURES_DEFAULT = System.getProperty(VARIABLE_USERHOME) + PATH_PICTURES;

	public static void main(String[] args) {
		int port = Integer.parseInt(ServerUtil.getProperty(KEY_SERVERPORT));
		String path = System.getenv(VARIABLE_PICTURES);
		String directory = null != path ? path : PATH_PICTURES_DEFAULT;
		CamCaptureServer server = new CamCaptureServer();
		server.start(port, directory);
	}

	private void start(int serverPort, String watcherDirectory) {
		LOG.info("Starting {} -> TCP {}", CamCaptureServer.class.getSimpleName(), serverPort);
		try {
			ServerThread serverThread = new ServerThread(serverPort);
			Thread sThread = new Thread(serverThread);
			sThread.start();
			WatcherThread watcherThread = new WatcherThread(watcherDirectory);
			watcherThread.addListener(serverThread);
			Thread wThread = new Thread(watcherThread);
			wThread.start();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			System.exit(-1);
		}
	}
}
