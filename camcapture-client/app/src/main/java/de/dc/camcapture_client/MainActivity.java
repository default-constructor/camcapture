package de.dc.camcapture_client;

import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import de.dc.camcapture_client.utils.PermissionRequestHandler;

public class MainActivity extends AppCompatActivity {

    private class ClientTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "do in background");
            try (Socket socket = new Socket(hostname, port);
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())
            ) {
                String serverAddress = socket.getInetAddress().toString();
                int serverPort = socket.getPort();
                Log.i(TAG, "Connection " + ++count + " with server " + serverAddress + ":" + serverPort);
                try {
                    String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                    dos.writeUTF("camcapture-client version " + version);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Cannot read version", e);
                }
                try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
                    while (true) {
                        buffer = (byte[]) ois.readObject();
                        if (0 == buffer.length) {
                            dos.writeUTF("you sent me bullshit!");
                            continue;
                        }
                        saveImage(buffer);
                        dos.writeUTF("thank you!");
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                Log.e(TAG, "Connection to server failed. Next try in 30 seconds.", e);
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException e1) {
                    // nothing to do...
                }
                executeClientTask(hostname, port);
            }
            return null;
        }

        private byte[] buffer;

        private int count = 0;

        private final String hostname;
        private final int port;

        private ClientTask(String hostname, int port) {
            this.hostname = hostname;
            this.port = port;
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
        Log.d(TAG, "on create");
        permissionRequestHandler.checkPermissions();
        if (!isExternalStorageWritable()) {
            // TODO: error handling
            return;
        }
        if (!DIRECTORY.mkdirs()) {
            Log.d(TAG, "Cannot create directory " + DIRECTORY_NAME);
        }
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(v -> startConnection());
        super.onCreate(savedInstanceState);
    }

    /**
     * Called after your activity has been stopped, prior to it being started again. Always followed
     * by onStart()
     */
    @Override
    protected void onRestart() {
        Log.d(TAG, "on restart");
        super.onRestart();
    }

    /**
     * Called when the activity is becoming visible to the user. Followed by onResume() if the
     * activity comes to the foreground, or onStop() if it becomes hidden.
     */
    @Override
    protected void onStart() {
        Log.d(TAG, "on start");
        super.onStart();
    }

    /**
     * Called when the activity will start interacting with the user. At this point your activity is
     * at the top of the activity stack, with user input going to it. Always followed by onPause().
     */
    @Override
    protected void onResume() {
        Log.d(TAG, "on resume");
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
        Log.d(TAG, "on pause");
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
        Log.d(TAG, "on stop");
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
        Log.d(TAG, "on destroy");
        super.onDestroy();
    }

    // END Main activity lifecycle

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionRequestHandler.handlePermissionsResult(requestCode, permissions, grantResults);
    }

    ImageView imageView;
    Button button;

    private ClientTask clientTask;

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

    private void saveImage(byte[] bytes) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = timestamp + ".jpg";
        File imageFile = new File(DIRECTORY, filename);
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            fos.write(bytes);
            fos.flush();
            MediaScannerConnection.scanFile(this, new String[]{imageFile.toString()}, null, null);
            Log.i(TAG, "Saved image " + imageFile.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startConnection() {
        AssetManager assetMgr = getAssets();
        try (InputStream is = assetMgr.open("client.properties")) {
            Properties props = new Properties();
            props.load(is);
            String hostname = props.getProperty("server.hostname");
            int port = Integer.parseInt(props.getProperty("server.port"));
            Log.i(TAG, "Connecting to " + hostname + ":" + port);
            executeClientTask(hostname, port);
        } catch (IOException e) {
            String msg = "Can't load client properties";
            Log.e(TAG, msg);
            throw new RuntimeException(msg, e);
        }
    }
}
