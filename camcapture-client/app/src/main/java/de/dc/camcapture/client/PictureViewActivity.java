package de.dc.camcapture.client;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

/**
 * @author Thomas Reno
 */
public class PictureViewActivity extends AppCompatActivity {

    public static final String TAG = PictureViewActivity.class.getSimpleName();

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
        setContentView(R.layout.activity_pictureview);
        super.onCreate(savedInstanceState);
    }

    ImageView imageView;
}
