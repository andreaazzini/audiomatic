package se.kth.id2012_project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.azzarcher.colormanager.ColorManager;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.EstimoteSDK;
import com.estimote.sdk.Region;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.IOException;
import java.util.List;



public class BeaconActivity extends ActionBarActivity {
    private static final String SERVER_IP = "130.229.161.101";
    private static final String SERVER_PORT = "8080";
    private static final String WEB_SERVER_NAME = "UbiquitousWebServer";
    private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private static final Region ALL_ESTIMOTE_BEACONS = new Region("regionId", ESTIMOTE_PROXIMITY_UUID, null, null);

    private Menu mMenu;
    private BeaconManager beaconManager;
    private MediaPlayer mMediaPlayer;
    private String mStreamingResourceUrl;
    private Event mEvent;
    private TCPClient mTCPClient;
    private ColorManager mColorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);

        // Set ColorManager
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        int toolbarColor = Color.argb(255, 33, 150, 243);
        int statusBarColor = ColorManager.generateDarkColorFromPrimary(toolbarColor);
        mColorManager = new ColorManager()
                .setToolbar(toolbar)
                .setToolbarColor(toolbarColor)
                .setStatusBarColor(getWindow(), statusBarColor);

        // App ID & App Token can be taken from App section of Estimote Cloud.
        EstimoteSDK.initialize(getApplicationContext(), "id2012-project", "30ff36944829f37eda7fe252493048d2");
        // Optional, debug logging.
        EstimoteSDK.enableDebugLogging(true);
        beaconManager = new BeaconManager(this);
        mMediaPlayer = new MediaPlayer();

        // Setup TCP client
        mTCPClient = new TCPClient(SERVER_IP);
        new Thread(mTCPClient).start();

        // Get the event name from EventActivity
        Intent fromEventActivity = getIntent();
        final String eventName = fromEventActivity.getStringExtra("event_name");
        toolbar.setTitle(eventName);
        mEvent = new Event(eventName);

        // Sets the action bar
        setSupportActionBar(toolbar);

        // Create global configuration and initialize ImageLoader with default config
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        final ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.init(config);

        // Sets the default Image View
        //ImageView imageView = (ImageView) findViewById(R.id.beaconImage);
        //imageView.setImageDrawable(getResources().getDrawable(R.drawable.nosignal));

        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {
                if (!beacons.isEmpty()) {
                    Beacon nearestBeacon = getNearestBeacon(beacons);

                    mTCPClient.send(ESTIMOTE_PROXIMITY_UUID.substring(0, 4));
                    String resourceName = mTCPClient.receive();
                    Resource resource = new Resource(
                            resourceName,
                            "http://" + SERVER_IP + ":" + SERVER_PORT + "/" + WEB_SERVER_NAME + "/" + eventName + "/" + resourceName + "/" + "audio.mp3",
                            "http://" + SERVER_IP + ":" + SERVER_PORT + "/" + WEB_SERVER_NAME + "/" + eventName + "/" + resourceName + "/" + "image.jpg");
                    // Save the resource for later fast retrieval
                    if (!mEvent.hasResourceOfBeacon(nearestBeacon.getProximityUUID())) {
                        mEvent.saveResource(nearestBeacon.getProximityUUID(), resource);
                    }
                    // Set the beaconImage
                    imageLoader.loadImage(resource.getImageUrl(), new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            ((ImageView) findViewById(R.id.beaconImage)).setImageBitmap(loadedImage);
                            int prominentColor = Palette.generate(loadedImage).getVibrantColor(Color.BLUE);
                            int prominentDarkColor = ColorManager.generateDarkColorFromPrimary(prominentColor);
                            // Let the animators work
                            try {
                                mColorManager.animateToolbar(prominentColor);
                                mColorManager.animateStatusBar(prominentDarkColor);
                            } catch (ColorManager.NoToolbarColorException e) {
                                e.printStackTrace();
                            } catch (ColorManager.NoStatusBarColorException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    // Stream the resource
                    try {
                        setBeaconAudioStream(resource.getAudioUrl());
                    } catch (IOException e) {
                        Log.d("BeaconActivity", "Can't stream the resource");
                    }
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS);
                } catch (RemoteException e) {
                    Log.e("Manager Start Error", "Cannot start ranging", e);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        try {
            beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS);
        } catch (RemoteException e) {
            Log.e("Manager Stop Error", "Cannot stop but it does not matter now", e);
        }
        beaconManager.disconnect();
        mTCPClient.closeConnection();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_beacon, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_play) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                item.setTitle("Play");
                item.setIcon(R.drawable.ic_play_arrow_black_48dp);
            } else {
                mMediaPlayer.start();
                item.setTitle("Pause");
                item.setIcon(R.drawable.ic_pause_black_48dp);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Beacon getNearestBeacon(List<Beacon> beacons) {
        // The nearest beacon is initialized as the first one in the list
        Beacon nearestBeacon = beacons.get(0);
        // Find the real nearest beacon
        for (Beacon beacon : beacons) {
            if (beacon.getRssi() > nearestBeacon.getRssi()) {
                nearestBeacon = beacon;
            }
        }
        return nearestBeacon;
    }

    private void setBeaconAudioStream(String path) throws IOException {
        if (!path.equals(mStreamingResourceUrl)) {
            // Plays the audio file
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            // Changes the audio icon in the toolbar
            MenuItem audioItem = mMenu.getItem(0);
            audioItem.setVisible(true);
            audioItem.setTitle("Pause");
            audioItem.setIcon(R.drawable.ic_pause_black_48dp);
            // Sets the new value of the streaming resource
            mStreamingResourceUrl = path;
        }
    }
}
