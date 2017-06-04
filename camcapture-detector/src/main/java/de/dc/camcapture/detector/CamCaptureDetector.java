package de.dc.camcapture.detector;

import static javax.swing.JFrame.EXIT_ON_CLOSE;
import static org.bytedeco.javacpp.avutil.AV_PIX_FMT_BGR24;
import static org.bytedeco.javacpp.avutil.AV_SAMPLE_FMT_S16;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_AA;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.equalizeHist;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_SCALE_IMAGE;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

/**
 * @author Thomas Reno
 */
public class CamCaptureDetector implements Runnable {

	public static enum Feature {
		EYE_TREE_EYEGLASSES, EYE, FRONTALFACE, FRONTALFACE_TREE, FULLBODY, LEFT_EYE_2SPLITS, LOWERBODY, PROFILEFACE, RIGHTEYE_2SPLITS, SMILE, UPPERBODY, PEDESTRIAN
	}
	
	private static final int IMAGE_WIDTH = 640;
	private static final int IMAGE_HEIGHT = 360;

	private static final int WEBCAM_DEVICE_INDEX = 0;

	private static final Properties PROPS = new Properties();

	static {
		try (InputStream is = CamCaptureDetector.class.getClassLoader().getResourceAsStream("detector.properties")) {
			PROPS.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static final String HAARCASCADE_PATH = "/cascades/haar";
	/** Face cascades */
	private static final String HAARCASCADE_EYE_TREE_EYEGLASSES_PATH = //
			HAARCASCADE_PATH + "/haarcascade_eye_tree_eyeglasses.xml";
	private static final String HAARCASCADE_EYE_PATH = //
			HAARCASCADE_PATH + "/haarcascade_eye.xml";
	private static final String HAARCASCADE_FRONTALFACE_ALT_PATH = //
			HAARCASCADE_PATH + "/haarcascade_frontalface_alt.xml";
	private static final String HAARCASCADE_FRONTALFACE_ALT2_PATH = //
			HAARCASCADE_PATH + "/haarcascade_frontalface_alt2.xml";
	private static final String HAARCASCADE_FRONTALFACE_ALT_TREE_PATH = //
			HAARCASCADE_PATH + "/haarcascade_frontalface_alt_tree.xml";
	private static final String HAARCASCADE_FRONTALFACE_DEFAULT_PATH = //
			HAARCASCADE_PATH + "/haarcascade_frontalface_default.xml";
	private static final String HAARCASCADE_LEFTEYE_2SPLITS_PATH = //
			HAARCASCADE_PATH + "/haarcascade_lefteye_2splits.xml";
	private static final String HAARCASCADE_PROFILEFACE_PATH = //
			HAARCASCADE_PATH + "/haarcascade_profileface.xml";
	private static final String HAARCASCADE_RIGHTEYE_2SPLITS_PATH = //
			HAARCASCADE_PATH + "/haarcascade_righteye_2splits.xml";
	private static final String HAARCASCADE_SMILE_PATH = //
			HAARCASCADE_PATH + "/haarcascade_smile.xml";

	/** Body cascades */
	private static final String HAARCASCADE_FULLBODY_PATH = //
			HAARCASCADE_PATH + "/haarcascade_fullbody.xml";
	private static final String HAARCASCADE_LOWERBODY_PATH = //
			HAARCASCADE_PATH + "/haarcascade_lowerbody.xml";
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
		System.out.println("starting cam-capture-detector...");
		new Thread(new CamCaptureDetector()).start();
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
						System.out.println("face detected!");
						sendEmail();
						String filename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".jpg";
						System.out.println(filename);
						File dir = new File(PROPS.getProperty("watcher.dir"));
						File file = new File(dir, filename);
						image = frameConverter.convert(capturedFrame);
						saveImage(file, image);
//						stream(capturedFrame);
						waitForMillis(5000L);
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
			e.printStackTrace();
		}
	}

	private void saveImage(File file, BufferedImage image) {
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

	private FFmpegFrameRecorder createFFmepFrameRecorder(File tempFile) throws FileNotFoundException {
		System.out.println("creating ffmpeg frame recorder...");
		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(new FileOutputStream(tempFile), IMAGE_WIDTH, IMAGE_HEIGHT, 2);
		recorder.setFormat("matroska");
		recorder.setPixelFormat(AV_PIX_FMT_BGR24);
		recorder.setVideoCodecName("libx264rgb");
		recorder.setVideoQuality(0);
		recorder.setSampleFormat(AV_SAMPLE_FMT_S16);
		recorder.setSampleRate(44100);
		recorder.setAudioCodecName("pcm_s16le");
		recorder.setTimestamp(System.currentTimeMillis());
		return recorder;
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
			}
			drawRectangle(videoMat, faces);
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
		grabber.setImageWidth(IMAGE_WIDTH);
		grabber.setImageHeight(IMAGE_HEIGHT);
		for (Entry<Feature, String> entry : classifierPaths.entrySet()) {
			String path = entry.getValue();
			Feature feature = entry.getKey();
			String classifierPath = getClass().getResource(path).getPath().substring(1);
			CascadeClassifier classifier = new CascadeClassifier(classifierPath);
			cascadeClassifiers.put(feature, classifier);
			if (classifier.isNull()) {
				try {
					throw new IOException("Fail on loading cascade classifier");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			absoluteFaceSizes.put(feature, 0);
		}
		canvas = new CanvasFrame("Capture Preview", CanvasFrame.getDefaultGamma() / grabber.getGamma());
		canvas.setSize(new Dimension(IMAGE_WIDTH, IMAGE_HEIGHT));
		canvas.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	private void recordFrame() {
		File tempFile = new File(Loader.getTempDir(), "camcapture.mkv");
		try {
			FFmpegFrameRecorder recorder = createFFmepFrameRecorder(tempFile);
			recorder.start();
			Frame[] frames = new Frame[1000];
			for (int n = 0; n < frames.length; n++) {
				Frame frame = new Frame(IMAGE_WIDTH, IMAGE_HEIGHT, Frame.DEPTH_UBYTE, 3);
				UByteIndexer frameIdx = frame.createIndexer();
				for (int i = 0; i < frameIdx.rows(); i++) {
					for (int j = 0; j < frameIdx.cols(); j++) {
						for (int k = 0; k < frameIdx.channels(); k++) {
							frameIdx.put(i, j, k, n + i + j + k);
						}
					}
				}
				recorder.record(frame);
				frames[n] = frame;
			}
			recorder.stop();
			recorder.release();

		} catch (FileNotFoundException | FrameRecorder.Exception e) {
			e.printStackTrace();
		}
	}

	private void sendEmail() {

	}

	private void stream(Frame capturedFrame) {
		recordFrame();
	}

	private void waitForMillis(long millis) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				previouslyDetected = false;
				timer.cancel();
			}
		}, millis);
	}
}
