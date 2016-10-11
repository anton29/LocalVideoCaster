package com.example.u1.localvideocaster;

import android.Manifest;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.CursorLoader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.IntroductoryOverlay;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
implements NavigationView.OnNavigationItemSelectedListener {


    String fileName;
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 1;
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 29;
//    ImageView imageView;
//    ImageButton videoControlButton;

    String selectedImagePath;
    File file;
    LocalServer localServer;
    Uri selectedImageUri;
    Bitmap ThumbImage;

    File temp;

    ThumbFragment fr;
    boolean iscasting;
    MediaMetadataRetriever retriever;
    CastContext mCastContext;
    private MediaRouteButton mMediaRouteButton;
    private CastStateListener mCastStateListener;
    private CastSession mCastSession;
    private SessionManagerListener<CastSession> mSessionManagerListener;
    private PlaybackLocation mLocation;
    private PlaybackState mPlaybackState;
    private static final String thumb = "image-480x270"; // "thumb";
    private static final String img = "image-780x1200";
    String ip;

    public enum PlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE
    }

    public enum PlaybackLocation {
        LOCAL,
        REMOTE
    }

    private IntroductoryOverlay mIntroductoryOverlay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        addFAB();


        mMediaRouteButton = (MediaRouteButton) findViewById(R.id.media_route_button);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), mMediaRouteButton);

        mCastContext = CastContext.getSharedInstance(this);
        mCastContext.registerLifecycleCallbacksBeforeIceCreamSandwich(this, savedInstanceState);



//        videoControlButton = (ImageButton) findViewById(R.id.videoControlButton);
//        videoControlButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                // Perform action on click
////                loadRemoteMedia(0,true);
////                play(0);
//
//            }
//        });

        retriever = new MediaMetadataRetriever();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        requestPermission();
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
         ip = android.text.format.Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

//        cast

        setupCastListener();
        mCastContext = CastContext.getSharedInstance(this);
        mCastContext.registerLifecycleCallbacksBeforeIceCreamSandwich(this, savedInstanceState);
        mCastSession = mCastContext.getSessionManager().getCurrentCastSession();

        mCastStateListener = new CastStateListener() {
            @Override
            public void onCastStateChanged(int newState) {
                if (newState != CastState.NO_DEVICES_AVAILABLE) {
                    showIntroductoryOverlay();
                }
            }
        };
        mCastContext = CastContext.getSharedInstance(this);

        if (mCastSession != null && mCastSession.isConnected()) {
            Log.d("mLocation","Remote");
            Log.d("mPlaybackState","idle");
        } else {
            Log.d("mLocation","local");
        }
        mPlaybackState = PlaybackState.IDLE;

    }

    private void addFAB(){
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), REQUEST_TAKE_GALLERY_VIDEO);
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    protected void onResume() {
        Log.d("method onResume", "On Resume Called");
        mCastContext.addCastStateListener(mCastStateListener);
        mCastContext.getSessionManager().addSessionManagerListener(
                mSessionManagerListener, CastSession.class);
        if (mCastSession != null && mCastSession.isConnected()) {
            Log.d("method onResume status","remote");
            if(selectedImagePath != null && fr != null){
                fr.updateViewCasting(selectedImagePath);
            }
        } else {
            Log.d("method onResume status","local");
            if(selectedImagePath != null && fr != null){
                fr.updateViewNotCasting(selectedImagePath);
            }
        }
        super.onResume();

    }



    @Override
    protected void onPause() {
        Log.d("method onPause", "On Pause Called");
        mCastContext.removeCastStateListener(mCastStateListener);
//        if(localServer != null) {
//            localServer.stop();
//        }
        //ReAdded
        mCastContext.getSessionManager().removeSessionManagerListener(
                mSessionManagerListener, CastSession.class);

        super.onPause();

    }

    @Override
    protected void onDestroy(){
        if(localServer != null){
            localServer.stop();
        }
        
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

         if (id == R.id.nav_gallery) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

            startActivityForResult(Intent.createChooser(intent,
                    "Select Picture"), REQUEST_TAKE_GALLERY_VIDEO);

        }else if(id == R.id.kill_server){
             if(localServer != null){
                 localServer.stop();
                 Log.i("Server", "Server stoped");
             }

         }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {


            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            String ip = android.text.format.Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            Log.i("ip", ip);
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {

                if(localServer != null){
                    localServer.stop();
                }
                    try {
                        localServer = new LocalServer();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                selectedImageUri = data.getData();
                selectedImagePath = getRealPathFromURI(selectedImageUri);
                if (selectedImagePath != null) {
//                    if(videoControlButton.getVisibility() == View.INVISIBLE){
//                        videoControlButton.setVisibility(View.VISIBLE);
//                        videoControlButton.setClickable(true);
//                    }
                    file = new File(getRealPathFromURI(selectedImageUri));
                    ThumbImage = ThumbnailUtils.createVideoThumbnail(getRealPathFromURI(selectedImageUri), MediaStore.Video.Thumbnails.MINI_KIND);
//                    imageView.setImageBitmap(ThumbImage);
                    if(ThumbImage != null){
                        temp =  makeFile(ThumbImage);
                        if(localServer!=null){
                            localServer.setVideoFile(file);
                            localServer.setImageFile(makeFile(Bitmap.createScaledBitmap(ThumbImage, 780, 1200, true)));
                        }
                    }

//                    localServer.setVideoFile(file);
//                    videoControlButton.setImageResource(R.drawable.ic_media_play);

                    Bundle bundle = new Bundle();
                    bundle.putString("ThumbNailPath", getRealPathFromURI(selectedImageUri));
                    bundle.putString("Title", fileName);
                    bundle.putBoolean("isCasting",iscasting);
                    Log.i("method onActivityResult", "new fragment created");
                    fr = new ThumbFragment();
                    fr.setArguments(bundle);
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction fragmentTransaction = fm.beginTransaction();
                    fragmentTransaction.replace(R.id.fooFragment,fr);
                    fragmentTransaction.commit();
                    boolean isConnected = (mCastSession != null)
                            && (mCastSession.isConnected() || mCastSession.isConnecting());
                    Log.i("method onActivityResult","is connected to cast device" + String.valueOf(isConnected));
                    if(isConnected){
                        loadRemoteMedia(0,true);
                    }


                }
            }
        }
    }


    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor returnCursor = getContentResolver().query(contentUri, null, null, null, null);
        CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        returnCursor.moveToFirst();
        String result = cursor.getString(column_index);
        fileName = returnCursor.getString(nameIndex);
        cursor.close();
        return result;
    }

    public File makeFile(Bitmap bitmap){
        //create a videoFile to write bitmap data
        File f = new File(getCacheDir(),"name" );
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Convert bitmap to byte array

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();

        //write the bytes in videoFile
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return f;
    }


    public void requestPermission() {
        List<String> permissionsNeeded = new ArrayList<String>();

        final List<String> permissionsList = new ArrayList<String>();
        if (!addPermission(permissionsList, Manifest.permission.READ_EXTERNAL_STORAGE))
            permissionsNeeded.add("external storage");
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_WIFI_STATE))
            permissionsNeeded.add("wifi");
        if (!addPermission(permissionsList, Manifest.permission.INTERNET))
            permissionsNeeded.add("internet");

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                String message = "You need to grant access to " + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);
                showMessageOKCancel(message,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (Build.VERSION.SDK_INT >= 23) {
                                    requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                            REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                                }
                            }
                        });
                return;
            }
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            }
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
            {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.ACCESS_WIFI_STATE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.INTERNET, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.ACCESS_NETWORK_STATE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted do anything that must have permission and can be done before user allows


                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Some Permission is Denied", Toast.LENGTH_SHORT)
                            .show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }



    private boolean addPermission(List<String> permissionsList, String permission) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permission);
                // Check for Rationale Option
                if (!shouldShowRequestPermissionRationale(permission))
                    return false;
            }
        }
        return true;
    }

//    cast

    private void loadRemoteMedia(int position, boolean autoPlay) {
        if (mCastSession == null) {
            return;
        }
        final RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            return;
        }
        remoteMediaClient.addListener(new RemoteMediaClient.Listener() {
            @Override
            public void onStatusUpdated() {
                Intent intent = new Intent(MainActivity.this, ExpandedControlsActivity.class);
                startActivity(intent);
                remoteMediaClient.removeListener(this);
            }

            @Override
            public void onMetadataUpdated() {
            }

            @Override
            public void onQueueStatusUpdated() {
            }

            @Override
            public void onPreloadStatusUpdated() {
            }

            @Override
            public void onSendingRemoteMediaRequest() {
            }
        });
        remoteMediaClient.load(buildMediaInfo(), autoPlay, position);
    }



    private MediaInfo buildMediaInfo() {
        retriever.setDataSource(selectedImagePath);
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, fileName);
        movieMetadata.addImage(new WebImage(Uri.parse("http://" + ip +":8080/image")));

        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        String videoURL = "http://" + ip +":8080/video";
        long timeInmillisec = Long.parseLong( time );
        Log.i("duration","video duration" +  String.valueOf(timeInmillisec));

        return new  MediaInfo.Builder( videoURL )
                .setContentType( "video/mp4" )
                .setStreamType( MediaInfo.STREAM_TYPE_BUFFERED )
                .setMetadata( movieMetadata )
                .build();
    }

    private void showIntroductoryOverlay() {
        if (mIntroductoryOverlay != null) {
            mIntroductoryOverlay.remove();
        }
        if ((mMediaRouteButton != null) && mMediaRouteButton.getVisibility() == View.VISIBLE) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    mIntroductoryOverlay = new IntroductoryOverlay.Builder(
                            MainActivity.this, mMediaRouteButton)
                            .setTitleText("Touch to cast media to your TV")
                            .setSingleTime()
                            .setOnOverlayDismissedListener(
                                    new IntroductoryOverlay.OnOverlayDismissedListener() {
                                        @Override
                                        public void onOverlayDismissed() {
                                            mIntroductoryOverlay = null;
                                        }
                                    })
                            .build();
                    mIntroductoryOverlay.show();
                }
            });
        }
    }

    private void play(int position) {
        mPlaybackState = PlaybackState.BUFFERING;
        mCastSession.getRemoteMediaClient().seek(position);
    }

    private void setupCastListener() {
        mSessionManagerListener = new SessionManagerListener<CastSession>() {

            @Override
            public void onSessionEnded(CastSession session, int error) {
                Log.i("cast","cast dissonnected onSessionEnded");
                onApplicationDisconnected();
            }

            @Override
            public void onSessionResumed(CastSession session, boolean wasSuspended) {
                onApplicationConnected(session);
            }

            @Override
            public void onSessionResumeFailed(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarted(CastSession session, String sessionId) {
                Log.i("cast","cast started onSessionStarted");
                onApplicationConnected(session);
            }

            @Override
            public void onSessionStartFailed(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarting(CastSession session) {}

            @Override
            public void onSessionEnding(CastSession session) {}

            @Override
            public void onSessionResuming(CastSession session, String sessionId) {}

            @Override
            public void onSessionSuspended(CastSession session, int reason) {}

            private void onApplicationConnected(CastSession castSession) {
                mCastSession = castSession;
                Log.i("cast","cast connected onApplicationConnected");
                if(selectedImagePath != null && fr != null){
                    fr.updateViewCasting(selectedImagePath);
                }
                if (null != fileName) {
                    if (mPlaybackState == PlaybackState.PLAYING) {
                        Log.d("onApplicationConnected ","mPlaybackState Playing");
//                        mVideoView.pause();
//                        loadRemoteMedia(mSeekbar.getProgress(), true);
                        finish();
                        return;
                    } else {
                        mPlaybackState = PlaybackState.IDLE;
                        Log.d("onApplicationConnected","mPlaybackState idle");
//                        updatePlaybackLocation(PlaybackLocation.REMOTE);
                    }
                }
//                updatePlayButton(mPlaybackState);
                invalidateOptionsMenu();
            }

            private void onApplicationDisconnected() {
                Log.i("cast","cast dissonnected onApplicationDisconnected");
                Log.d("ApplicationDisconnected","mLocation  local");
                Log.d("ApplicationDisconnected","mPlaybackState idle");
//              updatePlaybackLocation(PlaybackLocation.LOCAL);
                mPlaybackState = PlaybackState.IDLE;
                mLocation = PlaybackLocation.LOCAL;
                if(selectedImagePath != null && fr != null){
                    fr.updateViewNotCasting(selectedImagePath);
                }
                invalidateOptionsMenu();
            }
        };
    }




}
