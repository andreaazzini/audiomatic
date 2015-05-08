package se.kth.id2012_project;

import android.media.MediaPlayer;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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
    private MediaPlayer mMediaPlayer = new MediaPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);
        //  App ID & App Token can be taken from App section of Estimote Cloud.
        EstimoteSDK.initialize(getApplicationContext(), "id2012-project", "30ff36944829f37eda7fe252493048d2");
        // Optional, debug logging.
        EstimoteSDK.enableDebugLogging(true);

        beaconManager = new BeaconManager(this);

        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {
                if (!beacons.isEmpty()) {
                    Log.d("EventActivity", Integer.toString(beacons.size()));
                    Beacon nearestBeacon = getNearestBeacon(beacons);
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
        mMediaPlayer.setDataSource(path);
        mMediaPlayer.prepare();
        mMediaPlayer.start();
    }

    public void handlePlay(View view) {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            // TODO set button text to Play
        } else {
            mMediaPlayer.start();
            // TODO set button text to Pause
        }
    }
}
