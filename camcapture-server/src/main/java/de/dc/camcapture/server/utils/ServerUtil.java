package de.dc.camcapture.server.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Reno
 */
public final class ServerUtil {

	private static final Logger LOG = LoggerFactory.getLogger(ServerUtil.class);

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
				try (InputStream is = ServerUtil.class.getClassLoader().getResourceAsStream("server.properties")) {
					PROPS.load(is);
				}
			}
			return PROPS.getProperty(key);
		} catch (IOException e) {
			LOG.error("Cannot load server properties");
			throw new RuntimeException("Cannot load server properties");
		}
	}

	public static String hash(String string) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(string.getBytes());
			byte[] digest = md.digest();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < digest.length; i++) {
				sb.append(Integer.toHexString((digest[i] & 0xFF) | 0x100).toString());
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sleeps for the given milliseconds.
	 * 
	 * @param millis
	 *            long
	 */
	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// nothing to do...
		}
	}

	private ServerUtil() {
		// nothing to do...
	}

	public static boolean checkToken(String token) {
		String serverToken = PROPS.getProperty("server.token");
		return hash(serverToken).equals(token);
	}
}
