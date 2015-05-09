package se.kth.id2012_project;

import android.media.MediaPlayer;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.EstimoteSDK;
import com.estimote.sdk.Region;

import java.io.IOException;
import java.util.List;


public class BeaconActivity extends ActionBarActivity {
    private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private static final Region ALL_ESTIMOTE_BEACONS = new Region("regionId", ESTIMOTE_PROXIMITY_UUID, null, null);

    private BeaconManager beaconManager;
    private MediaPlayer mMediaPlayer;
    private String mStreamingResourceUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);
        //  App ID & App Token can be taken from App section of Estimote Cloud.
        EstimoteSDK.initialize(getApplicationContext(), "id2012-project", "30ff36944829f37eda7fe252493048d2");
        // Optional, debug logging.
        EstimoteSDK.enableDebugLogging(true);

        beaconManager = new BeaconManager(this);
        mMediaPlayer = new MediaPlayer();

        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {
                if (!beacons.isEmpty()) {
                    Log.d("EventActivity", Integer.toString(beacons.size()));
                    Beacon nearestBeacon = getNearestBeacon(beacons);
                    // TODO retrieve the correct resource
                    Button beaconButton = (Button) findViewById(R.id.beaconButton);
                    beaconButton.setClickable(true);
                    String resourceUrl = "http://a1083.phobos.apple.com/us/r1000/014/Music/v4/4e/44/b7/4e44b7dc-aaa2-c63b-fb38-88e1635b5b29/mzaf_1844128138535731917.plus.aac.p.m4a";
                    try {
                        setBeaconAudioStream(resourceUrl);
                    } catch (IOException e) {
                        Log.d("BeaconActivity", "PORCO DIO");
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
