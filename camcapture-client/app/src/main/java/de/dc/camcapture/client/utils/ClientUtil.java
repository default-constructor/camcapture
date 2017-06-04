package de.dc.camcapture.client.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

/**
 * @author Thomas Reno
 */
public final class ClientUtil {

    public static final String TAG = ClientUtil.class.getSimpleName();

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

    public static void logPermissions(String tag, String[] permissions) {
        for (String permission : permissions) {
            Log.d(tag, "permission: " + permission);
        }
    }

    public static String hashByMD5(String string) {
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

    private ClientUtil() {
        // nothing to do...
    }
}
