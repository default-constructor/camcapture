package de.dc.camcapture.client.utils;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;

/**
 * @author Thomas Reno
 */
public final class ClientUtil {

    public static final String TAG = ClientUtil.class.getSimpleName();

    /**
     * Converts the given milliseconds as timestamp to a formatted <code>String</code>.
     *
     * @param millis  long
     * @param pattern String
     * @return the formatted date string
     */
    public static String convert(long millis, String pattern) {
        return convert(new Date(millis), pattern);
    }

    /**
     * Converts the given <code>Date</code> to a formatted <code>String</code>.
     *
     * @param date    Date
     * @param pattern String
     * @return the formatted date string
     */
    public static String convert(Date date, String pattern) {
        DateFormat formatter = new SimpleDateFormat(pattern);
        return formatter.format(date);
    }

    /**
     * Converts the given <code>String</code> object to a <code>Date</code>.
     *
     * @param string  String
     * @param pattern String
     * @return the date object
     */
    public static Date convert(String string, String pattern) {
        DateFormat formatter = new SimpleDateFormat(pattern);
        Date date = null;
        try {
            date = formatter.parse(string);
        } catch (ParseException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return date;
    }

    /**
     * Gets the date and time of the given milliseconds as timestamp for the notification.
     *
     * @param millis long
     * @return the date and time
     */
    public static String getDateTimeForNotification(long millis) {
        Calendar now = GregorianCalendar.getInstance();
        Calendar then = new GregorianCalendar();
        then.setTimeInMillis(millis);
        if (then.get(YEAR) == now.get(YEAR)
                && then.get(MONTH) == now.get(MONTH)
                && then.get(DAY_OF_MONTH) == now.get(DAY_OF_MONTH)) {
            return convert(millis, "HH:mm:ss");
        }
        return convert(millis, "dd.MM.yyyy HH:mm:ss");
    }

    private static final Properties PROPS = new Properties();

    /**
     * Gets property value from the given key.
     *
     * @param key String
     * @return the property value
     */
    public static String getProperty(String key, AssetManager assetMgr) {
        try {
            if (PROPS.isEmpty()) {
                try (InputStream is = assetMgr.open("client.properties")) {
                    PROPS.load(is);
                }
            }
            return PROPS.getProperty(key);
        } catch (IOException e) {
            Log.e(TAG, "Cannot load server properties");
            throw new RuntimeException("Cannot load server properties");
        }
    }

    /**
     * Hashes a <code>String</code> by the MD5 algorythm.
     *
     * @param string String
     * @return the hashed string
     */
    public static String hash(String string) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(string.getBytes());
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(Integer.toHexString((b & 0xFF) | 0x100));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private ClientUtil() {
        // nothing to do...
    }
}
