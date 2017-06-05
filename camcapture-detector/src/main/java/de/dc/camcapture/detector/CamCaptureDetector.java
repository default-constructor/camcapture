package de.dc.camcapture.detector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dc.camcapture.detector.threads.DetectorThread;

/**
 * @author Thomas Reno
 */
public class CamCaptureDetector {

	private static final Logger LOG = LoggerFactory.getLogger(CamCaptureDetector.class);

	private static final int WEBCAM_DEVICE_INDEX = 0;

	public static void main(String[] args) {
		CamCaptureDetector detector = new CamCaptureDetector();
		detector.start();
	}	

	private void start() {
		LOG.info("Starting {} listening on device {}", CamCaptureDetector.class.getSimpleName(), WEBCAM_DEVICE_INDEX);
		Thread serverThread = new Thread(new DetectorThread(WEBCAM_DEVICE_INDEX));
		serverThread.start();
	}
}
