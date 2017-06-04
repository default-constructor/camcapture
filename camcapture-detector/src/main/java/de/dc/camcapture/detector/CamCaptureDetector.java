package de.dc.camcapture.detector;

import static javax.swing.JFrame.EXIT_ON_CLOSE;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_AA;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.equalizeHist;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_SCALE_IMAGE;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dc.camcapture.detector.utils.DetectorUtil;

/**
 * @author Thomas Reno
 */
public class CamCaptureDetector implements Runnable {

	public static enum Feature {
		EYE_TREE_EYEGLASSES, EYE, FRONTALFACE, FRONTALFACE_TREE, FULLBODY, LEFT_EYE_2SPLITS, LOWERBODY, PROFILEFACE, RIGHTEYE_2SPLITS, SMILE, UPPERBODY, PEDESTRIAN
	}

	private static final Logger LOG = LoggerFactory.getLogger(CamCaptureDetector.class);

	private static final int IMAGE_WIDTH = 640;
	private static final int IMAGE_HEIGHT = 360;

	private static final int WEBCAM_DEVICE_INDEX = 0;

	private static final String HAARCASCADE_PATH = "/cascades/haar";
	/** Face cascades */
	@SuppressWarnings("unused")
	private static final String HAARCASCADE_EYE_TREE_EYEGLASSES_PATH = //
			HAARCASCADE_PATH + "/haarcascade_eye_tree_eyeglasses.xml";
	@SuppressWarnings("unused")
	private static final String HAARCASCADE_EYE_PATH = //
			HAARCASCADE_PATH + "/haarcascade_eye.xml";
	@SuppressWarnings("unused")
	private static final String HAARCASCADE_FRONTALFACE_ALT_PATH = //
			HAARCASCADE_PATH + "/haarcascade_frontalface_alt.xml";
	@SuppressWarnings("unused")
	private static final String HAARCASCADE_FRONTALFACE_ALT2_PATH = //
			HAARCASCADE_PATH + "/haarcascade_frontalface_alt2.xml";
	@SuppressWarnings("unused")
	private static final String HAARCASCADE_FRONTALFACE_ALT_TREE_PATH = //
			HAARCASCADE_PATH + "/haarcascade_frontalface_alt_tree.xml";
	private static final String HAARCASCADE_FRONTALFACE_DEFAULT_PATH = //
			HAARCASCADE_PATH + "/haarcascade_frontalface_default.xml";
	@SuppressWarnings("unused")
	private static final String HAARCASCADE_LEFTEYE_2SPLITS_PATH = //
			HAARCASCADE_PATH + "/haarcascade_lefteye_2splits.xml";
	@SuppressWarnings("unused")
	private static final String HAARCASCADE_PROFILEFACE_PATH = //
			HAARCASCADE_PATH + "/haarcascade_profileface.xml";
	@SuppressWarnings("unused")
	private static final String HAARCASCADE_RIGHTEYE_2SPLITS_PATH = //
			HAARCASCADE_PATH + "/haarcascade_righteye_2splits.xml";
	@SuppressWarnings("unused")
	private static final String HAARCASCADE_SMILE_PATH = //
			HAARCASCADE_PATH + "/haarcascade_smile.xml";

	/** Body cascades */
	@SuppressWarnings("unused")
	private static final String HAARCASCADE_FULLBODY_PATH = //
			HAARCASCADE_PATH + "/haarcascade_fullbody.xml";
	@SuppressWarnings("unused")
	private static final String HAARCASCADE_LOWERBODY_PATH = //
			HAARCASCADE_PATH + "/haarcascade_lowerbody.xml";
	@SuppressWarnings("unused")
	private static final String HAARCASCADE_UPPERBODY_PATH = //
			HAARCASCADE_PATH + "/haarcascade_upperbody.xml";

	private static HashMap<Feature, String> classifierPaths = new HashMap<>();

	static {
		// classifierPaths.put(Feature.FRONTALFACE,
		// HAARCASCADE_EYE_TREE_EYEGLASSES_PATH);
		// classifierPaths.put(Feature.FRONTALFACE, HAARCASCADE_EYE_PATH);
		// classifierPaths.put(Feature.FRONTALFACE,
		// HAARCASCADE_FRONTALFACE_ALT_PATH);
		// classifierPaths.put(Feature.FRONTALFACE,
		// HAARCASCADE_FRONTALFACE_ALT2_PATH);
		// classifierPaths.put(Feature.FRONTALFACE,
		// HAARCASCADE_FRONTALFACE_ALT_TREE_PATH);
		classifierPaths.put(Feature.FRONTALFACE, HAARCASCADE_FRONTALFACE_DEFAULT_PATH);
		// classifierPaths.put(Feature.FRONTALFACE,
		// HAARCASCADE_LEFTEYE_2SPLITS_PATH);
		// classifierPaths.put(Feature.FRONTALFACE,
		// HAARCASCADE_PROFILEFACE_PATH);
		// classifierPaths.put(Feature.FRONTALFACE,
		// HAARCASCADE_RIGHTEYE_2SPLITS_PATH);
		// classifierPaths.put(Feature.FRONTALFACE, HAARCASCADE_SMILE_PATH);
	}

	static {
		// classifierPaths.put(Feature.FRONTALFACE, HAARCASCADE_FULLBODY_PATH);
		// classifierPaths.put(Feature.FRONTALFACE, HAARCASCADE_LOWERBODY_PATH);
		// classifierPaths.put(Feature.UPPERBODY, HAARCASCADE_UPPERBODY_PATH);
	}

	public static void main(String[] args) {
		new Thread(new CamCaptureDetector()).start();
	}

	@Override
	public void run() {
		LOG.info("Starting [{}]", CamCaptureDetector.class.getSimpleName());
		try {
			Loader.load(opencv_objdetect.class);
			grabber.start();
			Frame capturedFrame;
			while (null != (capturedFrame = grabber.grab())) {
				if (detectFaces(converterToMat.convert(capturedFrame), new Mat())) {
					if (!previouslyDetected) {
						LocalDateTime now = LocalDateTime.now();
						LOG.info("Detection [{}]", DetectorUtil.format(now, "yyyy-MM-dd HH:mm:ss"));
						String filename = DetectorUtil.format(now, "yyyyMMdd_HHmmss") + ".jpg";
						File dir = new File(DetectorUtil.getProperty("watcher.dir"));
						File file = new File(dir, filename);
						image = frameConverter.convert(capturedFrame);
						saveImage(file, image);
						// After 5 seconds detections going to be saved again
						DetectorUtil.schedule(5000L, () -> previouslyDetected = false);
					}
					previouslyDetected = true;
				}
				if (canvas.isVisible()) {
					canvas.showImage(capturedFrame);
				}
			}
			canvas.dispose();
			grabber.stop();
		} catch (FrameGrabber.Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private void saveImage(File file, BufferedImage image) {
		LOG.info("Saving [{}]", file);
		try {
			ImageIO.write(image, "jpg", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(WEBCAM_DEVICE_INDEX);

	private OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
	private Java2DFrameConverter frameConverter = new Java2DFrameConverter();

	private HashMap<Feature, CascadeClassifier> cascadeClassifiers = new HashMap<>();
	private HashMap<Feature, Integer> absoluteFaceSizes = new HashMap<>();

	private CanvasFrame canvas;

	private boolean previouslyDetected = false;

	private BufferedImage image;

	public CamCaptureDetector() {
		init();
	}

	private boolean detectFaces(Mat videoMat, Mat videoMatGray) {
		boolean detected = false;
		cvtColor(videoMat, videoMatGray, COLOR_BGR2GRAY);
		equalizeHist(videoMatGray, videoMatGray);
		for (Entry<Feature, CascadeClassifier> entry : cascadeClassifiers.entrySet()) {
			Feature feature = entry.getKey();
			CascadeClassifier classifier = entry.getValue();
			int absFaceSize = absoluteFaceSizes.get(feature);
			if (0 == absFaceSize) {
				int height = videoMatGray.rows();
				int size;
				if (0 < (size = (int) Math.round(height * 0.2))) {
					absoluteFaceSizes.replace(feature, size);
				}
			}
			RectVector faces = new RectVector();
			classifier.detectMultiScale(videoMatGray, faces, 1.1, 20, 0 | CV_HAAR_SCALE_IMAGE,
					new Size(absFaceSize, absFaceSize), new Size());
			if (0 < faces.size()) {
				detected = true;
				drawRectangle(videoMat, faces);
			}
		}
		return detected;
	}

	private void drawRectangle(Mat videoMat, RectVector faces) {
		for (int i = 0; i < faces.size(); i++) {
			Rect rect = faces.get(i);
			rectangle(videoMat, rect, new Scalar(0, 255, 0, 1), 2, CV_AA, 0);
		}
	}

	private void init() {
		LOG.info("Initializing detector [WIDTH {}px / HEIGHT {}px]", IMAGE_WIDTH, IMAGE_HEIGHT);
		grabber.setImageWidth(IMAGE_WIDTH);
		grabber.setImageHeight(IMAGE_HEIGHT);
		for (Entry<Feature, String> entry : classifierPaths.entrySet()) {
			String path = entry.getValue();
			Feature feature = entry.getKey();
			LOG.info("Loading cascade classifier [{}] [{}]", feature, path);
			String classifierPath = getClass().getResource(path).getPath().substring(1);
			CascadeClassifier classifier = new CascadeClassifier(classifierPath);
			cascadeClassifiers.put(feature, classifier);
			if (classifier.isNull()) {
				try {
					throw new IOException("Fail on loading cascade classifier");
				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
				}
			}
			absoluteFaceSizes.put(feature, 0);
		}
		canvas = new CanvasFrame("Capture Preview", CanvasFrame.getDefaultGamma() / grabber.getGamma());
		canvas.setSize(new Dimension(IMAGE_WIDTH, IMAGE_HEIGHT));
		canvas.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
}
