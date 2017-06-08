package de.dc.camcapture.client.activities;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

import java.io.File;

import de.dc.camcapture.client.R;
import de.dc.camcapture.client.fragments.ImageFragment;
import de.dc.camcapture.client.utils.ClientUtil;

/**
 * @author Thomas Reno
 */
public class PictureViewActivity extends FragmentActivity {

    private static class ImageFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

        private static final String TAG = PictureViewActivity.class.getSimpleName();

        @Override
        public int getCount() {
            return files.length;
        }

        @Override
        public Fragment getItem(int position) {
            return ImageFragment.create(files[position]);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            String filename = files[position].getName();
            String string = filename.substring(0, filename.indexOf("."));
            String dateTime = ClientUtil.convert(ClientUtil.convert(string, "yyyyMMdd_HHmmss"), "dd.MM.yyyy HH:mm:ss");
            Log.d(TAG, dateTime);
            return "Unbekannt vom " + dateTime;
        }

        private ImageFragmentStatePagerAdapter(FragmentManager fm) {
            super(fm);
        }
    }

    public static final String TAG = PictureViewActivity.class.getSimpleName();

    private static final String DIRECTORY_NAME = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/CamCapture";

    private static File[] files;

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
        setContentView(R.layout.activity_pictureview);
        File directory = new File(DIRECTORY_NAME);
        files = directory.listFiles();
        ImageFragmentStatePagerAdapter fragmentStatePagerAdapter =
                new ImageFragmentStatePagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(fragmentStatePagerAdapter);
        viewPager.setCurrentItem(files.length - 1);
        String filename = getIntent().getExtras().getString("snapshot");
    }
}
