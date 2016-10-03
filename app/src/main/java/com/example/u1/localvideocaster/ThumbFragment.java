package com.example.u1.localvideocaster;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;

/**
 * Created by U1 on 9/23/2016.
 */
public class ThumbFragment extends Fragment {
    Bundle bundle;
    Bitmap ThumbImage;
    ImageView imageView;
    TextView descriptionTextView;
    TextView title;
    VideoView videoView;
    boolean isCasting;
    String path;
    SeekBar seekbar;
    MediaController mediaController;

    public boolean isCasting() {return isCasting;}
    public void setCasting(boolean casting) {isCasting = casting;}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.thumb_nail_fragment,
                container, false);
        Log.v("new","23");
        bundle = this.getArguments();
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        Bundle bundle = this.getArguments();


        imageView = (ImageView) getView().findViewById(R.id.videoThumbFragment);
        videoView = (VideoView) getView().findViewById(R.id.videoView1);
        descriptionTextView = (TextView) getView().findViewById(R.id.descriptionTextView);
        title = (TextView) getView().findViewById(R.id.titleTextView);
        title.setText((String) bundle.get("Title"));

        displayType();

//        imageView.setImageBitmap(bundle);
//        Log.v("bundle", String.valueOf(bundle));
//        String path = (String) bundle.get("ThumbNailPath");
//        ThumbImage = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND);
//        imageView.setImageBitmap(ThumbImage);

    }

    public void updateViewCasting(String path){
        videoView.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.VISIBLE);
        ThumbImage = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND);
        imageView.setImageBitmap(ThumbImage);
        Log.v("casting","Y");
    }

    public void updateViewNotCasting(){
        videoView.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.INVISIBLE);
        Log.v("bundle", String.valueOf(bundle));
        mediaController = new MediaController(getActivity());
        videoView.setVideoPath(path);
    }
    
    public void displayType(){
        WifiManager wm = (WifiManager) getActivity().getSystemService(getActivity().WIFI_SERVICE);
        String ip = android.text.format.Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        path = (String) bundle.get("ThumbNailPath");
        isCasting = (boolean) bundle.get("isCasting");
        descriptionTextView.setText("Your LocalServer " + ip + ":8080");
//        isCasting = isCasting();
        if(isCasting){
//            videoView.setVisibility(View.INVISIBLE);
//            imageView.setVisibility(View.VISIBLE);
//            ThumbImage = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND);
//            imageView.setImageBitmap(ThumbImage);
//            Log.v("casting","Y");
            updateViewCasting(path);

        }else{
//            videoView.setVisibility(View.VISIBLE);
//            imageView.setVisibility(View.INVISIBLE);
//            Log.v("bundle", String.valueOf(bundle));
//            mediaController = new MediaController(getActivity());
//            videoView.setVideoPath(path);
            updateViewNotCasting();

            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.v("string","1");
                    mediaController = new MediaController(getActivity());
                    videoView.setMediaController(mediaController);
                /*
                 * and set its position on screen
                 */
                    mediaController.setAnchorView(videoView);
                    videoView.seekTo(0);

                    mediaController.show(60*1000);

                }
            });




            Log.v("casting","N");
        }
    }



}
