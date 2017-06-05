package de.dc.camcapture.detector;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dc.camcapture.detector.threads.DetectorThread;

/**
 * @author Thomas Reno
 */
public class CamCaptureDetector {

	private static final Logger LOG = LoggerFactory.getLogger(CamCaptureDetector.class);

	private static final int CAMERA_INDEX = 0;

	private static final int CAMERA_WIDTH = 640;
	private static final int CAMERA_HEIGHT = 360;

	public static void main(String[] args) {
		CamCaptureDetector detector = new CamCaptureDetector();
		detector.start();
	}

	private void start() {
		LOG.info("Starting {} listening on device {}", CamCaptureDetector.class.getSimpleName(), CAMERA_INDEX);
		String defaultPath = System.getProperty("user.home") + "/CamCapture/Pictures";
		String path = System.getenv("CAMCAPTURE_PICTURES");
		File dir = new File(null != path ? path : defaultPath);
		dir.mkdirs();
		Thread serverThread = new Thread(new DetectorThread(CAMERA_INDEX, CAMERA_WIDTH, CAMERA_HEIGHT, dir));
		serverThread.start();
	}
}
