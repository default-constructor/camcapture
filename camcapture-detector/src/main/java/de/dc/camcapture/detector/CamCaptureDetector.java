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
	
	private static final String VARIABLE_USERHOME = "user.home";
	private static final String VARIABLE_PICTURES = "camcapture.pictures";

	private static final String PATH_PICTURES = "/CamCapture/Pictures";
	private static final String PATH_PICTURES_DEFAULT = System.getProperty(VARIABLE_USERHOME) + PATH_PICTURES;

	private static final int CAMERA_INDEX = 0;

	private static final int CAMERA_WIDTH = 640;
	private static final int CAMERA_HEIGHT = 360;

	public static void main(String[] args) {
		CamCaptureDetector detector = new CamCaptureDetector();
		detector.start();
	}

	private void start() {
		LOG.info("Starting {} -> Camera index {}", CamCaptureDetector.class.getSimpleName(), CAMERA_INDEX);
		String path = System.getenv(VARIABLE_PICTURES);
		File directory = new File(null != path ? path : PATH_PICTURES_DEFAULT);
		directory.mkdirs();
		DetectorThread detectorThread = new DetectorThread(CAMERA_INDEX, CAMERA_WIDTH, CAMERA_HEIGHT, directory);
		Thread dThread = new Thread(detectorThread);
		dThread.start();
	}
}
