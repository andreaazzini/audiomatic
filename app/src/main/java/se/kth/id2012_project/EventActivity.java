package se.kth.id2012_project;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class EventActivity extends ActionBarActivity {
    private TCPClient mTCPClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        mTCPClient = new TCPClient();
        new Thread(mTCPClient).start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_event, menu);
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

    public void confirmCode(View v) {
        EditText codeText = (EditText) findViewById(R.id.eventNumber);
        String code = codeText.getText().toString();
        mTCPClient.send(code);
        String response = mTCPClient.receive();
        if (response.equals("ERROR")) {
            codeText.setError("The specified code does not exist");
        } else {
            mTCPClient.closeConnection();
            Intent confirmIntent = new Intent(this, BeaconActivity.class);
            confirmIntent.putExtra("event_name", response);
            startActivity(confirmIntent);
            finish();
        }
    }
}
