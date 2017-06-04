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
			throw new RuntimeException(e);
		}
	}

	/**
	 * Hashes a string by the MD5 algorithm.
	 * 
	 * @param string
	 *            String
	 * @return the hashed string
	 */
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

	/**
	 * Validates the given token, if its equals the server token.
	 * 
	 * @param token
	 *            String
	 * @return if the token is valid
	 */
	public static void validateToken(String address, String token) {
		String serverToken = PROPS.getProperty("server.token");
		if (!token.equals(hash(serverToken))) {
			LOG.error("Access denied [{}]", address);
			throw new IllegalArgumentException("Invalid token");
		}
	}

	private ServerUtil() {
		// nothing to do...
	}
}
