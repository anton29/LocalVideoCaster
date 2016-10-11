package com.example.u1.localvideocaster;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.text.method.LinkMovementMethod;
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
        bundle = this.getArguments();
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
        Log.i("view status","hide video");
        videoView.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.VISIBLE);
        descriptionTextView.setVisibility(View.VISIBLE);
        ThumbImage = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND);
        imageView.setImageBitmap(ThumbImage);
    }

    public void updateViewNotCasting(String path){
        Log.i("view status","show video");
        videoView.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.INVISIBLE);
        descriptionTextView.setVisibility(View.INVISIBLE);
        mediaController = new MediaController(getActivity());
        videoView.setVideoPath(path);
    }
    
    public void displayType(){
        WifiManager wm = (WifiManager) getActivity().getSystemService(getActivity().WIFI_SERVICE);
        String ip = android.text.format.Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        path = (String) bundle.get("ThumbNailPath");
        isCasting = (boolean) bundle.get("isCasting");
//        String text = "<a href='http:"+ip+":/8080/video'> Google </a>";
//        descriptionTextView.setClickable(true);
//        descriptionTextView.setMovementMethod(LinkMovementMethod.getInstance());
//        descriptionTextView.setText( Html.fromHtml(text));
        descriptionTextView.setText( "Your Server is running on: http:"+ip+":/8080/video" );
//        isCasting = isCasting();
        if(isCasting){
            updateViewCasting(path);

        }else{
            updateViewNotCasting(path);

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

                    mediaController.show(3*1000);

                }
            });
        }
    }



}
