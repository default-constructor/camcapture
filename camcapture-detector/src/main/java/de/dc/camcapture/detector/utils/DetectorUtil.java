package de.dc.camcapture.detector.utils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Reno
 */
public final class DetectorUtil {

	private static final Logger LOG = LoggerFactory.getLogger(DetectorUtil.class);

	private static final Properties PROPS = new Properties();

	/**
	 * Gets property value from the given key.
	 * 
	 * @param key
	 *            String
	 * @return the property value
	 */
	public static String getProperty(String key) {
		try {
			if (PROPS.isEmpty()) {
				try (InputStream is = DetectorUtil.class.getClassLoader().getResourceAsStream("detector.properties")) {
					PROPS.load(is);
				}
			}
			return PROPS.getProperty(key);
		} catch (IOException e) {
			LOG.error("Cannot load detector properties");
			throw new RuntimeException("Cannot load detector properties");
		}
	}

	/**
	 * Schedules the given milliseconds and calls <code>isScheduled()</code>
	 * from the given {@link SchedulerListener}, when the timer task is over.
	 * 
	 * @param millis
	 *            long
	 * @param listener
	 *            {@link SchedulerListener}
	 */
	public static void schedule(long millis, SchedulerListener listener) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				listener.isScheduled();
				timer.cancel();
			}
		}, millis);
	}

	/**
	 * Formats the given <code>LocalDateTime</code> object to the given form.
	 * 
	 * @param dateTime
	 *            LocalDateTime
	 * @param pattern
	 *            String
	 * @return the formatted <code>LocalDateTime</code>
	 */
	public static String format(LocalDateTime dateTime, String pattern) {
		return dateTime.format(DateTimeFormatter.ofPattern(pattern));
	}
}
