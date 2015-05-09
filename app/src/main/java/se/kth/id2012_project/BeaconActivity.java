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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
    private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private static final Region ALL_ESTIMOTE_BEACONS = new Region("regionId", ESTIMOTE_PROXIMITY_UUID, null, null);

    private BeaconManager beaconManager;
    private MediaPlayer mMediaPlayer;
    private String mStreamingResourceUrl;
    private Event mEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);
        // Set ActionBar
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // App ID & App Token can be taken from App section of Estimote Cloud.
        EstimoteSDK.initialize(getApplicationContext(), "id2012-project", "30ff36944829f37eda7fe252493048d2");
        // Optional, debug logging.
        EstimoteSDK.enableDebugLogging(true);
        beaconManager = new BeaconManager(this);
        mMediaPlayer = new MediaPlayer();
        // Get the event name from EventActivity
        Intent fromEventActivity = getIntent();
        String eventName = fromEventActivity.getStringExtra("event_name");
        toolbar.setTitle(eventName);
        mEvent = new Event(eventName);
        // Create global configuration and initialize ImageLoader with default config
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        final ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.init(config);

        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {
                if (!beacons.isEmpty()) {
                    Log.d("EventActivity", Integer.toString(beacons.size()));
                    Beacon nearestBeacon = getNearestBeacon(beacons);
                    // TODO retrieve the correct resource
                    Resource resource = new Resource(
                            "Happy",
                            "http://a1083.phobos.apple.com/us/r1000/014/Music/v4/4e/44/b7/4e44b7dc-aaa2-c63b-fb38-88e1635b5b29/mzaf_1844128138535731917.plus.aac.p.m4a",
                            "http://upload.wikimedia.org/wikipedia/en/2/23/Pharrell_Williams_-_Happy.jpg");
                    // Save the resource for later fast retrieval
                    if (!mEvent.hasResourceOfBeacon(nearestBeacon.getProximityUUID())) {
                        mEvent.saveResource(nearestBeacon.getProximityUUID(), resource);
                    }
                    // Set the beaconName
                    ((TextView) findViewById(R.id.beaconName)).setText(resource.getName());
                    // Set the beaconImage
                    // OLD imageLoader.displayImage(resource.getImageUrl(), (ImageView) findViewById(R.id.beaconImage));
                    imageLoader.loadImage(resource.getImageUrl(), new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            ((ImageView) findViewById(R.id.beaconImage)).setImageBitmap(loadedImage);
                            int prominentColor = Palette.generate(loadedImage).getVibrantColor(Color.BLACK);
                            toolbar.setBackgroundColor(prominentColor);
                        }
                    });
                    // Stream the resource
                    try {
                        setBeaconAudioStream(resource.getAudioUrl());
                        // Set the beaconButton visible
                        findViewById(R.id.beaconButton).setVisibility(View.VISIBLE);
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
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_beacon, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
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
            // Sets the new value of the streaming resource
            mStreamingResourceUrl = path;
        }
    }

    public void handlePlay(View view) {
        Button beaconButton = (Button) findViewById(R.id.beaconButton);
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            beaconButton.setText("Play");
        } else {
            mMediaPlayer.start();
            beaconButton.setText("Pause");
        }
    }
}
