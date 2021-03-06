package de.dc.camcapture.detector.threads;

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
public class DetectorThread implements Runnable {

	public static enum Feature {
		EYE_TREE_EYEGLASSES, EYE, FRONTALFACE, FRONTALFACE_TREE, FULLBODY, LEFT_EYE_2SPLITS, LOWERBODY, PROFILEFACE, RIGHTEYE_2SPLITS, SMILE, UPPERBODY, PEDESTRIAN
	}

	private static final Logger LOG = LoggerFactory.getLogger(DetectorThread.class);

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

	@Override
	public void run() {
		try {
			Loader.load(opencv_objdetect.class);
			grabber.start();
			Frame capturedFrame;
			while (null != (capturedFrame = grabber.grab())) {
				if (detectFaces(converterToMat.convert(capturedFrame), new Mat())) {
					if (!previouslyDetected) {
						LocalDateTime now = LocalDateTime.now();
						LOG.info("Detected object at {}", DetectorUtil.format(now, "yyyy-MM-dd HH:mm:ss"));
						String filename = DetectorUtil.format(now, "yyyyMMdd_HHmmss") + ".jpg";
						image = frameConverter.convert(capturedFrame);
						saveSnapshot(filename, image);
						// After 5 seconds detections going to be saveable again
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

	private CanvasFrame canvas;

	private boolean previouslyDetected = false;

	private BufferedImage image;

	private final OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
	private final Java2DFrameConverter frameConverter = new Java2DFrameConverter();

	private final HashMap<Feature, CascadeClassifier> cascadeClassifiers = new HashMap<>();
	private final HashMap<Feature, Integer> absoluteFaceSizes = new HashMap<>();

	private final OpenCVFrameGrabber grabber;
	private final File directory;
	private final int width;
	private final int height;

	public DetectorThread(int camIndex, int width, int height, File directory) {
		grabber = new OpenCVFrameGrabber(camIndex);
		this.directory = directory;
		this.width = width;
		this.height = height;
		try {
			init();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
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

	private void init() throws IOException {
		grabber.setImageWidth(width);
		grabber.setImageHeight(height);
		for (Entry<Feature, String> entry : classifierPaths.entrySet()) {
			String path = entry.getValue();
			Feature feature = entry.getKey();
			String classifierPath = getClass().getResource(path).getPath().substring(1);
			CascadeClassifier classifier = new CascadeClassifier(classifierPath);
			cascadeClassifiers.put(feature, classifier);
			if (classifier.isNull()) {
				throw new IOException("Fail on loading cascade classifier");
			}
			absoluteFaceSizes.put(feature, 0);
			LOG.info("Loaded {} cascade classifier from {}", feature, path);
		}
		canvas = new CanvasFrame("Capture Preview", CanvasFrame.getDefaultGamma() / grabber.getGamma());
		canvas.setSize(new Dimension(width, height));
		canvas.setDefaultCloseOperation(EXIT_ON_CLOSE);
		LOG.info("Initialized detector with {}px x {}px", width, height);
	}

	private void saveSnapshot(String filename, BufferedImage snapshot) {
		try {
			File file = new File(directory, filename);
			ImageIO.write(snapshot, "jpg", file);
			LOG.info("Saved snapshot {}", filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
