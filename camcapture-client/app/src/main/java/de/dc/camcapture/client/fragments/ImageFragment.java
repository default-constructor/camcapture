package de.dc.camcapture.client.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import de.dc.camcapture.client.R;

/**
 * @author Thomas Reno
 */
public class ImageFragment extends Fragment {

    public static ImageFragment create(File file) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("file", file);
        ImageFragment fragment = new ImageFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (null != bundle) {
            File file = (File) bundle.getSerializable("file");
            filename = file.getName();
            bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
        } else {
            filename = "No image";
            bmp = BitmapFactory.decodeResource(getResources(), R.drawable.no_image_available);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_image, container, false);
        TextView textView = (TextView) root.findViewById(R.id.textView);
        textView.setText("Image fragment for " + filename);
        imageView = (ImageView) root.findViewById(R.id.imageView);
        imageView.setImageBitmap(bmp);
        return root;
    }

    private ImageView imageView;

    private Bitmap bmp;
    private String filename;
}
