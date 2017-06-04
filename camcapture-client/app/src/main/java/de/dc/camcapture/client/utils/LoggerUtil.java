package de.dc.camcapture.client.utils;

import android.util.Log;

/**
 * @author Thomas Reno
 */
public final class LoggerUtil {

    public static void logPermissions(String tag, String[] permissions) {
        for (String permission : permissions) {
            Log.d(tag, "permission: " + permission);
        }
    }

    private LoggerUtil() {
        // nothing to do...
    }
}
