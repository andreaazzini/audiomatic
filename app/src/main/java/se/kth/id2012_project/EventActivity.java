package se.kth.id2012_project;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class EventActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
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
        // TODO check the existence of the code
        // if so, get the beacon information
        // and go to the BeaconActivity through an Intent
        Intent confirmIntent = new Intent(this, BeaconActivity.class);
        // TODO retrieve the real event name
        confirmIntent.putExtra("event_name", "Happiness Museum");
        startActivity(confirmIntent);
        finish();
        // otherwise, set error
    }
}
