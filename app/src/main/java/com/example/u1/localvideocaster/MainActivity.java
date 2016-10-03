package com.example.u1.localvideocaster;

import android.Manifest;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.MediaRouteButton;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.graphics.Bitmap;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Patterns;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.Session;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;

public class MainActivity extends FragmentActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private CastSession mCastSession;
    private SessionManager mSessionManager;
    private final SessionManagerListener mSessionManagerListener =
            new SessionManagerListenerImpl();
    String fileName;
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 1;
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 29;
//    ImageView imageView;
    ImageButton videoControlButton;
    ImageButton startCasting;
    String selectedImagePath;
    File file;
    LocalServer localServer;
    Uri selectedImageUri;
    Bitmap ThumbImage;

    ThumbFragment fr;
    boolean iscasting;
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;
    private CastDevice mSelectedDevice;
    private GoogleApiClient mApiClient;
    private RemoteMediaPlayer mRemoteMediaPlayer;
    private Cast.Listener mCastClientListener;
    private boolean mWaitingForReconnect = false;
    private boolean mApplicationStarted = false;
    private boolean mVideoIsLoaded;
    private boolean mIsPlaying;
    MediaMetadataRetriever retriever;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSessionManager = CastContext.getSharedInstance(this).getSessionManager();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        MediaRouteButton mMediaRouteButton = (MediaRouteButton) findViewById(R.id.media_route_button);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), mMediaRouteButton);

        videoControlButton = (ImageButton) findViewById(R.id.videoControlButton);

        videoControlButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                if( !mVideoIsLoaded ){
                    startVideo();
                    Log.v("cast", "start video");
                } else{
                    controlVideo();
                }

            }
        });

        retriever = new MediaMetadataRetriever();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        requestPermission();

//        cast
        initMediaRouter();

    }

    @Override
    protected void onResume() {
        mCastSession = mSessionManager.getCurrentCastSession();
        mSessionManager.addSessionManagerListener(mSessionManagerListener);
        super.onResume();

        mMediaRouter.addCallback( mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN );

        Log.v("file", String.valueOf(file));
        Log.v("resume", "On Resume Called");
    }



    @Override
    protected void onPause() {
        Log.v("pause", "On Pause Called");
//        if(localServer != null) {
//            localServer.stop();
//        }

        if ( isFinishing() ) {
            // End media router discovery
            mMediaRouter.removeCallback( mMediaRouterCallback );
        }
        super.onPause();
        mSessionManager.removeSessionManagerListener(mSessionManagerListener);
        mCastSession = null;
    }

    @Override
    protected void onDestroy(){
        if(localServer != null){
            localServer.stop();
        }
        teardown();
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

//        cast
//        MenuItem mediaRouteMenuItem = menu.findItem( R.id.media_route_menu_item );
//        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat.getActionProvider( mediaRouteMenuItem );
//        mediaRouteActionProvider.setRouteSelector( mMediaRouteSelector );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

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

        } else if (id == R.id.nav_recent_media) {

         }else if(id == R.id.kill_server){
             if(localServer != null){
                 localServer.stop();
                 Log.v("Server", "Server stoped");
             }

         }

//        } else if (id == R.id.nav_manage) {
//
//        } else if (id == R.id.nav_share) {
//
//        } else if (id == R.id.nav_send) {
//
//        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
//            if(localServer != null){
//                localServer.stop();
//                Log.v("local","server not null");
//            }

            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            String ip = android.text.format.Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            Log.v("ip", ip);
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
                mVideoIsLoaded = false;
                if(localServer != null){
                    localServer.stop();
                    Log.v("local","server not null");
                }
                    try {
                        localServer = new LocalServer();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                selectedImageUri = data.getData();

                // OI FILE Manager
//                String filemanagerstring = selectedImageUri.getPath();

                // MEDIA GALLERY
                // selectedImagePath = getPath(selectedImageUri);
//                Log.v("path",""+selectedImageUri);
//                Log.v("path",""+filemanagerstring);
//                Log.v("path",""+getRealPathFromURI(selectedImageUri));
                selectedImagePath = getRealPathFromURI(selectedImageUri);
                if (selectedImagePath != null) {
                    Log.v("vis", String.valueOf(videoControlButton.getVisibility()));
                    if(videoControlButton.getVisibility() == View.INVISIBLE){
                        videoControlButton.setVisibility(View.VISIBLE);
                        videoControlButton.setClickable(true);
                    }
                    file = new File(getRealPathFromURI(selectedImageUri));
                    ThumbImage = ThumbnailUtils.createVideoThumbnail(getRealPathFromURI(selectedImageUri), MediaStore.Video.Thumbnails.MICRO_KIND);
//                    imageView.setImageBitmap(ThumbImage);
                    localServer.setFile(file);
                    videoControlButton.setImageResource(R.drawable.ic_media_play);

                    Bundle bundle = new Bundle();
                    bundle.putString("ThumbNailPath", getRealPathFromURI(selectedImageUri));
                    bundle.putString("Title", fileName);
                    bundle.putBoolean("isCasting",iscasting);
                    Log.v("new", "new fragment");
                    fr = new ThumbFragment();
                    fr.setArguments(bundle);
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction fragmentTransaction = fm.beginTransaction();
                    fragmentTransaction.replace(R.id.fooFragment,fr);
                    fragmentTransaction.commit();

//                    try {
////                        file = new File(Environment.getExternalStorageDirectory() + "/DCIM/Camera/20160905_235301.mp4");
//                        localServer = new LocalServer();
//                        localServer.setFile(file);
//                        localServer.start();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }

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
//        itemname = GetFiles(Environment.getExternalStorageDirectory() + "/DCIM/Camera");
//        new MyTask(this).execute(itemname);
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
                    // All Permissions Granted
//                    itemname = GetFiles(Environment.getExternalStorageDirectory() + "/DCIM/Camera");
//                    new MyTask(this).execute(itemname);
//
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


    private void initMediaRouter() {
        // Configure Cast device discovery
        mMediaRouter = MediaRouter.getInstance( getApplicationContext() );
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory( CastMediaControlIntent.categoryForCast( CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID ) )
                .build();
        mMediaRouterCallback = new MediaRouterCallback();
    }

    private void initCastClientListener() {
        mCastClientListener = new Cast.Listener() {
            @Override
            public void onApplicationStatusChanged() {
            }

            @Override
            public void onVolumeChanged() {
            }

            @Override
            public void onApplicationDisconnected( int statusCode ) {
                teardown();
            }
        };
    }

    private void initRemoteMediaPlayer() {
        mRemoteMediaPlayer = new RemoteMediaPlayer();
        mRemoteMediaPlayer.setOnStatusUpdatedListener( new RemoteMediaPlayer.OnStatusUpdatedListener() {
            @Override
            public void onStatusUpdated() {
                MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
                mIsPlaying = mediaStatus.getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING;
            }
        });

        mRemoteMediaPlayer.setOnMetadataUpdatedListener( new RemoteMediaPlayer.OnMetadataUpdatedListener() {
            @Override
            public void onMetadataUpdated() {
            }
        });
    }

    private void controlVideo() {
        if( mRemoteMediaPlayer == null || !mVideoIsLoaded )
            return;

        if( mIsPlaying ) {
            mRemoteMediaPlayer.pause( mApiClient );
            videoControlButton.setImageResource(R.drawable.ic_media_play);
        } else {
            mRemoteMediaPlayer.play( mApiClient );
            videoControlButton.setImageResource(R.drawable.ic_media_pause);
        }
    }

    private void startVideo()
    {

        MediaMetadata mediaMetadata = new MediaMetadata( MediaMetadata.MEDIA_TYPE_MOVIE );
        retriever.setDataSource(selectedImagePath);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, fileName);
        Uri path = Uri.parse("android.resource://" + getPackageName() + "/" + + R.drawable.cast_ic_notification_play);
        mediaMetadata.addImage(new WebImage(path));
//        mediaMetadata.addImage(new WebImage(Uri.parse(bigImageUrl)));
//        mediaMetadata.putString( MediaMetadata.KEY_TITLE, getString( R.string.video_title ) );
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = android.text.format.Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        Log.v("ip", ip);
        String videoURL = "http://" + ip +":8080";
        Log.v("isValid", String.valueOf(Patterns.WEB_URL.matcher(videoURL).matches()));

        MediaInfo mediaInfo = new MediaInfo.Builder( videoURL )
                .setContentType( "video/mp4" )
                .setStreamType( MediaInfo.STREAM_TYPE_BUFFERED )
                .setMetadata( mediaMetadata )
                .build();

        Toast.makeText(getApplicationContext(), "Video build " + videoURL, Toast.LENGTH_LONG).show();

        try
        {
            mRemoteMediaPlayer.load( mApiClient, mediaInfo, true )
                    .setResultCallback( new ResultCallback<RemoteMediaPlayer.MediaChannelResult>()
                    {
                        @Override
                        public void onResult( RemoteMediaPlayer.MediaChannelResult mediaChannelResult )
                        {
                            try {
                                if( mediaChannelResult.getStatus().isSuccess() )
                                {
                                    mVideoIsLoaded = true;
                                    videoControlButton.setImageResource(R.drawable.ic_media_pause);

                                    Toast.makeText(getApplicationContext(), "Media loaded successfully", Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.v("error", "error" + e);
                            }
                        }
                    } );
        }
        catch (IllegalStateException e)
        {
            Log.v("error", "error" + e);
            Toast.makeText(getApplicationContext(), "Problem occurred with media during loading : " + e, Toast.LENGTH_LONG).show();
        }
        catch (Exception e)
        {
            Log.v("error", "error" + e);
            Toast.makeText(getApplicationContext(), "Problem occurred with media during loading : " + e, Toast.LENGTH_LONG).show();
        }
    }

    private class MediaRouterCallback extends MediaRouter.Callback {

        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo info) {
            initCastClientListener();
            initRemoteMediaPlayer();

            mSelectedDevice = CastDevice.getFromBundle( info.getExtras() );

            launchReceiver();
        }

        @Override
        public void onRouteUnselected( MediaRouter router, RouteInfo info ) {
            teardown();
            mSelectedDevice = null;
            videoControlButton.setImageResource(R.drawable.ic_media_play);
            mVideoIsLoaded = false;
        }
    }

    private void launchReceiver() {
        Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                .builder( mSelectedDevice, mCastClientListener );

        ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks();
        ConnectionFailedListener mConnectionFailedListener = new ConnectionFailedListener();
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Cast.API, apiOptionsBuilder.build() )
                .addConnectionCallbacks( mConnectionCallbacks )
                .addOnConnectionFailedListener( mConnectionFailedListener )
                .build();

        mApiClient.connect();
    }

    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {

        @Override
        public void onConnected( Bundle hint ) {
            if( mWaitingForReconnect ) {
                mWaitingForReconnect = false;
                reconnectChannels( hint );
            } else {
                try {
                    Cast.CastApi.launchApplication( mApiClient, CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID, false )
                            .setResultCallback( new ResultCallback<Cast.ApplicationConnectionResult>() {
                                                    @Override
                                                    public void onResult(Cast.ApplicationConnectionResult applicationConnectionResult) {
                                                        Status status = applicationConnectionResult.getStatus();
                                                        if( status.isSuccess() ) {
                                                            //Values that can be useful for storing/logic
                                                            ApplicationMetadata applicationMetadata = applicationConnectionResult.getApplicationMetadata();
                                                            String sessionId = applicationConnectionResult.getSessionId();
                                                            String applicationStatus = applicationConnectionResult.getApplicationStatus();
                                                            boolean wasLaunched = applicationConnectionResult.getWasLaunched();

                                                            mApplicationStarted = true;
                                                            reconnectChannels( null );
                                                        }
                                                    }
                                                }
                            );
                } catch ( Exception e ) {

                }
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            mWaitingForReconnect = true;
        }
    }

    private void reconnectChannels( Bundle hint ) {
        if( ( hint != null ) && hint.getBoolean( Cast.EXTRA_APP_NO_LONGER_RUNNING ) ) {
            //Log.e( TAG, "App is no longer running" );
            teardown();
        } else {
            try {
                Cast.CastApi.setMessageReceivedCallbacks( mApiClient, mRemoteMediaPlayer.getNamespace(), mRemoteMediaPlayer );
            } catch( IOException e ) {
                //Log.e( TAG, "Exception while creating media channel ", e );
            } catch( NullPointerException e ) {
                //Log.e( TAG, "Something wasn't reinitialized for reconnectChannels" );
            }
        }
    }

    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed( ConnectionResult connectionResult ) {
            teardown();
        }
    }

    private void teardown() {
        if( mApiClient != null ) {
            if( mApplicationStarted ) {
                try {
                    Cast.CastApi.stopApplication( mApiClient );
                    if( mRemoteMediaPlayer != null ) {
                        Cast.CastApi.removeMessageReceivedCallbacks( mApiClient, mRemoteMediaPlayer.getNamespace() );
                        mRemoteMediaPlayer = null;
                    }
                } catch( IOException e ) {
                    //Log.e( TAG, "Exception while removing application " + e );
                }
                mApplicationStarted = false;
            }
            if( mApiClient.isConnected() )
                mApiClient.disconnect();
            mApiClient = null;
        }
        mSelectedDevice = null;
        mVideoIsLoaded = false;
    }


    private class SessionManagerListenerImpl implements SessionManagerListener {

        @Override
        public void onSessionStarted(Session session, String sessionId) {
            Log.v("session","start");
            if(fr != null){
                fr.setCasting(true);
            }
            iscasting = true;
            invalidateOptionsMenu();
        }

        @Override
        public void onSessionResumed(Session session, boolean wasSuspended) {
            Log.v("session","resume");
            invalidateOptionsMenu();
        }

        @Override
        public void onSessionEnded(Session session, int error) {
            Log.v("session","end");
            if(fr != null){
                fr.setCasting(false);
            }
            iscasting = false;
            if(localServer != null) {
                localServer.stop();
                Log.v(" server ", "stop server");
            }
//            finish();
        }

        @Override
        public void onSessionResuming(Session session, String s) {}
        @Override
        public void onSessionStarting(Session session) {}
        @Override
        public void onSessionStartFailed(Session session, int i) {}
        @Override
        public void onSessionEnding(Session session) {}
        @Override
        public void onSessionResumeFailed(Session session, int i) {}
        @Override
        public void onSessionSuspended(Session session, int i) {}
    }



}
