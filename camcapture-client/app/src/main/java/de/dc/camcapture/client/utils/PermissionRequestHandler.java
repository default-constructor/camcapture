package de.dc.camcapture.client.utils;

import android.Manifest;
import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.util.Arrays;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.support.v4.content.ContextCompat.checkSelfPermission;

/**
 * @author Thomas Reno
 */
public class PermissionRequestHandler {

    public static final String TAG = PermissionRequestHandler.class.getSimpleName();

    private static final int PERMISSION_REQUEST_SAVE_IMAGE = 0;

    private boolean permissionGranted;

    private final Activity context;

    public PermissionRequestHandler(Activity context) {
        this.context = context;
    }

    public void checkPermissions() {
        Log.d(TAG, "check permissions");
        int checkAccessNetworkState = checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE);
        int checkInternet = checkSelfPermission(context, Manifest.permission.INTERNET);
        int checkRead = checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        int checkWrite = checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (!(PERMISSION_GRANTED == (checkAccessNetworkState | checkInternet | checkRead | checkWrite))) {
            String[] permissions = new String[]{
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            context.requestPermissions(permissions, PERMISSION_REQUEST_SAVE_IMAGE);
        }
    }

    public void handlePermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "handle permissions result");
        switch (requestCode) {
            case PERMISSION_REQUEST_SAVE_IMAGE:
                Log.d(TAG, "permission request save image");
                permissionGranted = Arrays.stream(grantResults)
                        .allMatch(grantResult -> PERMISSION_GRANTED == grantResult);
                break;
            default:
                // nothing to do...
        }
        Log.d(TAG, "permission granted: " + permissionGranted);
        if (!permissionGranted) {
            AlertDialog.Builder adBuilder = new AlertDialog.Builder(context);
            AlertDialog permissionDialog = adBuilder
                    .setTitle("App requirements")
                    .setMessage("This app needs permissions granted.")
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        context.finish();
                        System.exit(0);
                    })
                    .create();
            permissionDialog.show();
        }
    }
}
