package se.kth.id2012_project;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
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
    private Toolbar mToolbar;
    private int mToolbarColor;
    private int mStatusBarColor;
    private TCPClient mTCPClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);
        // Set ActionBar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mStatusBarColor = Color.argb(255, 25, 118, 210);
        mToolbarColor = Color.argb(255, 33, 150, 243);
        getWindow().setStatusBarColor(mStatusBarColor);
        // App ID & App Token can be taken from App section of Estimote Cloud.
        EstimoteSDK.initialize(getApplicationContext(), "id2012-project", "30ff36944829f37eda7fe252493048d2");
        // Optional, debug logging.
        EstimoteSDK.enableDebugLogging(true);
        beaconManager = new BeaconManager(this);
        mMediaPlayer = new MediaPlayer();
        // Get the event name from EventActivity
        Intent fromEventActivity = getIntent();
        String eventName = fromEventActivity.getStringExtra("event_name");
        // Setup TCP client
        mTCPClient = new TCPClient();
        new Thread(mTCPClient).start();
        mToolbar.setTitle(eventName);

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
                    mTCPClient.send(ESTIMOTE_PROXIMITY_UUID.substring(0, 4));
                    String resourceName = mTCPClient.receive();
                    Log.d("BeaconActivity", resourceName);
                    Resource resource = new Resource(
                            resourceName,
                            "http://a1083.phobos.apple.com/us/r1000/014/Music/v4/4e/44/b7/4e44b7dc-aaa2-c63b-fb38-88e1635b5b29/mzaf_1844128138535731917.plus.aac.p.m4a",
                            "http://a4.mzstatic.com/us/r30/Music/v4/3a/e9/f1/3ae9f18b-1ec0-d5a7-c452-9af373f52762/886444405560.600x600-75.jpg");
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
                            final int prominentColor = Palette.generate(loadedImage).getVibrantColor(mToolbarColor);
                            // Darken prominent color
                            float[] hsv = new float[3];
                            Color.colorToHSV(prominentColor, hsv);
                            hsv[2] *= 0.8f; // value component
                            final int prominentDarkColor = Color.HSVToColor(hsv);
                            // Let the animators work
                            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), mToolbarColor, prominentColor);
                            ValueAnimator colorStatusAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), mStatusBarColor, prominentDarkColor);
                            colorAnimation.setDuration(1000);
                            colorStatusAnimation.setDuration(1000);
                            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animator) {
                                    int transitionColor = (int) animator.getAnimatedValue();
                                    mToolbar.setBackgroundColor(transitionColor);
                                    mToolbarColor = transitionColor;
                                }
                            });
                            colorStatusAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animator) {
                                    int transitionColor = (int) animator.getAnimatedValue();
                                    getWindow().setStatusBarColor(transitionColor);
                                    mStatusBarColor = transitionColor;
                                }
                            });
                            colorAnimation.start();
                            colorStatusAnimation.start();
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
        mTCPClient.closeConnection();
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
