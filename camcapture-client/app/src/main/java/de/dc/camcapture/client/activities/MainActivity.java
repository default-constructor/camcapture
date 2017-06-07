package de.dc.camcapture.client.activities;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import de.dc.camcapture.client.R;
import de.dc.camcapture.client.utils.PermissionRequestHandler;

import static de.dc.camcapture.client.utils.ClientUtil.*;

/**
 * @author Thomas Reno
 */
public class MainActivity extends AppCompatActivity {

    private class ClientTask extends AsyncTask<Void, Void, Void> {

        private static final String CLIENT_TOKEN = "client.token";

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "do in background");
            try (Socket socket = new Socket()) {
                SocketAddress socketAddress = new InetSocketAddress(hostname, port);
                socket.connect(socketAddress, 10000);
                Log.i(TAG, "Connection " + ++count + " with server " + hostname + ":" + port);
                try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                     InputStream is = socket.getInputStream();
                     ObjectInputStream ois = new ObjectInputStream(is)
                ) {
                    while (true) {
                        dos.writeUTF(hashedToken);
                        dos.flush();
                        long timestamp;
                        if (/* Connectivity-check */ -1L == (timestamp = ois.readLong())) {
                            continue;
                        }
                        String filename = ois.readUTF();
                        bufferContent = (byte[]) ois.readObject();
                        if (0 == bufferContent.length) {
                            Log.e(TAG, "Response without content [" + filename + "]");
                            continue;
                        }
                        saveImage(filename, bufferContent);
                        showNotification(timestamp, filename);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                Log.e(TAG, "Connection to server failed. Next try in 10 seconds.");
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e1) {
                    // nothing to do...
                }
                executeClientTask(hostname, port);
            }
            return null;
        }

        private byte[] bufferContent;

        private int count = 0;

        private final String hostname;
        private final int port;
        private final String hashedToken;

        private ClientTask(String hostname, int port) {
            this.hostname = hostname;
            this.port = port;
            String token = getProperty(CLIENT_TOKEN, assetManager);
            hashedToken = hash(token);
        }
    }

    public static final String TAG = MainActivity.class.getSimpleName();

    private static final String DIRECTORY_NAME = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/CamCapture";
    private static final File DIRECTORY = new File(DIRECTORY_NAME);

    /*
     * Main activity lifecycle
     */

    /**
     * Called when the activity is first created. This is where you should do all of your normal
     * static set up: create views, bind data to lists, etc. This method also provides you with a
     * Bundle containing the activity's previously frozen state, if there was one. Always followed
     * by onStart().
     *
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "on create");
        assetManager = getAssets();
        permissionRequestHandler.checkPermissions();
        if (!isExternalStorageWritable()) {
            // TODO: error handling
            return;
        }
        if (!DIRECTORY.mkdirs()) {
            Log.d(TAG, "Cannot create directory " + DIRECTORY_NAME);
        }
        setContentView(R.layout.activity_main);
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.startConnection();
            }
        });
    }

    /**
     * Called after your activity has been stopped, prior to it being started again. Always followed
     * by onStart()
     */
    @Override
    protected void onRestart() {
        super.onRestart();
    }

    /**
     * Called when the activity is becoming visible to the user. Followed by onResume() if the
     * activity comes to the foreground, or onStop() if it becomes hidden.
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * Called when the activity will start interacting with the user. At this point your activity is
     * at the top of the activity stack, with user input going to it. Always followed by onPause().
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Called when the system is about to start resuming a previous activity. This is typically used
     * to commit unsaved changes to persistent data, stop animations and other things that may be
     * consuming CPU, etc. Implementations of this method must be very quick because the next
     * activity will not be resumed until this method returns.
     * Followed by either onResume() if the activity returns back to the front, or onStop() if it
     * becomes invisible to the user.
     */
    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Called when the activity is no longer visible to the user, because another activity has been
     * resumed and is covering this one. This may happen either because a new activity is being
     * started, an existing one is being brought in front of this one, or this one is being
     * destroyed.
     * Followed by either onRestart() if this activity is coming back to interact with the user, or
     * onDestroy() if this activity is going away.
     */
    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * The final call you receive before your activity is destroyed. This can happen either because
     * the activity is finishing (someone called finish() on it, or because the system is
     * temporarily destroying this instance of the activity to save space. You can distinguish
     * between these two scenarios with the isFinishing() method.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // END Main activity lifecycle

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionRequestHandler.handlePermissionsResult(requestCode, permissions, grantResults);
    }

    private int notificationId = 0;

    private ClientTask clientTask;

    private AssetManager assetManager;

    private final PermissionRequestHandler permissionRequestHandler;

    public MainActivity() {
        this.permissionRequestHandler = new PermissionRequestHandler(this);
    }

    private void executeClientTask(String hostname, int port) {
        if (null == clientTask || !clientTask.isCancelled()) {
            if (null != clientTask) {
                clientTask.cancel(true);
            }
            clientTask = new ClientTask(hostname, port);
        }
        clientTask.execute();
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void saveImage(String filename, byte[] bytes) {
        Log.d(TAG, "filename: " + filename);
        File file = new File(DIRECTORY, filename);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
            fos.flush();
            MediaScannerConnection.scanFile(this, new String[]{file.toString()}, null, null);
            Log.i(TAG, "Saved image " + file.toString());
        } catch (FileNotFoundException e) {
            Log.d(TAG, "file not found exception");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "input output exception");
            e.printStackTrace();
        }
    }

    private void showNotification(long timestamp, String filename) {
        Intent intent = new Intent(this, PictureViewActivity.class);
        intent.putExtra("image", filename);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(PictureViewActivity.class);
        stackBuilder.addNextIntent(intent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.cam_128)
                .setContentTitle("CamCapture Detection (" + getDateTimeForNotification(timestamp) + ")")
                .setContentText("Es wurde jemand von der Kamera erfasst.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(notificationId++, builder.build());
    }

    private void startConnection() {
        String hostname = getProperty("server.hostname", assetManager);
        int port = Integer.parseInt(getProperty("server.port", assetManager));
        Log.i(TAG, "Connecting to " + hostname + ":" + port);
        executeClientTask(hostname, port);
    }
}
