package com.example.jon.servicelab;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private DataUpdateReciever mDataUpdateReceiver;
    private Button mResultsButton;
    private Button mStartButton;
    private int mNumberOfResults = 0;
    private List<String> mResultStrings = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //note because we are using an earlier SDK level, you will need to cast here.
        mResultsButton = (Button) findViewById(R.id.resultsButton);

        mStartButton = (Button) findViewById(R.id.startButton);

        SharedPreferences sharedPreferences =
                getSharedPreferences(getString(R.string.keys_shared_prefs),
                        Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();


        mStartButton.setOnClickListener(button -> {
            DemoIntentService.startServiceAlarm(this, true);
            editor.putBoolean(getString(R.string.keys_sp_on), true);
            editor.apply();
            mStartButton.setEnabled(false);
        });

        mResultsButton.setOnClickListener(this::checkResults);

        findViewById(R.id.stopButton).setOnClickListener(button -> {
            DemoIntentService.stopServiceAlarm(this);
            editor.putBoolean(getString(R.string.keys_sp_on), false);
            editor.apply();
            mStartButton.setEnabled(true);
        });
    }

    public void checkResults(View theButton) {
        LinearLayout layout = (LinearLayout )findViewById(R.id.resultsLayout);
        mResultsButton.setEnabled(false);
        mResultsButton.setText(getString(R.string.check));
        mNumberOfResults = 0;
        for (String s : mResultStrings) {
            TextView textView = new TextView(this);
            textView.setText(parseJson(s));
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            layout.addView(textView);
        }
        mResultStrings.clear();
    }


    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences =
                getSharedPreferences(getString(R.string.keys_shared_prefs),
                        Context.MODE_PRIVATE);
        // Check to see if the service should aleardy be running
        if (sharedPreferences.getBoolean(getString(R.string.keys_sp_on), false)) {
            //stop the service from the background
            DemoIntentService.stopServiceAlarm(this);
            //restart but in the foreground
            DemoIntentService.startServiceAlarm(this, true);
            mStartButton.setEnabled(false);
        }

        //Look to see if the intent has a result string for us.
        //If true, then this Activity was started fro the notification bar
        if (getIntent().hasExtra(getString(R.string.keys_extra_results))) {
            LinearLayout layout = (LinearLayout )findViewById(R.id.resultsLayout);
            TextView textView = new TextView(this);
            //get a substring of the JSON
            textView.setText(getIntent()
                    .getStringExtra(getString(R.string.keys_extra_results))
                    .substring(85, 115));
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            layout.addView(textView);
        }

        //Re-instantiates an API update receiver
        if (mDataUpdateReceiver == null) {
            mDataUpdateReceiver = new DataUpdateReciever();
        }
        IntentFilter iFilter = new IntentFilter(DemoIntentService.RECEIVED_UPDATE);
        registerReceiver(mDataUpdateReceiver, iFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences sharedPreferences =
                getSharedPreferences(getString(R.string.keys_shared_prefs),
                        Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(getString(R.string.keys_sp_on), false)) {
            //stop the service from the foreground
            DemoIntentService.stopServiceAlarm(this);
            //restart but in the background
            DemoIntentService.startServiceAlarm(this, false);
        }

        if (mDataUpdateReceiver != null){
            unregisterReceiver(mDataUpdateReceiver);
        }

    }

    /**
     * Gets some information from all this data.
     * The input is expected to use the format based on the documentation found :
     * https://phishnet.api-docs.io/v3/setlists/get-a-random-phish-setlist
     *
     * @param jsonResult a JSON String
     * @return the data and location of the show
     */
    private String parseJson(final String jsonResult) {
        String result = "";

        try {
            JSONObject json = new JSONObject(jsonResult);
            if (json.has("error_code") && json.getInt("error_code") == 0) {
                if (json.has("response")) {
                    JSONObject response = json.getJSONObject("response");
                    if (response.has("data")) {
                        JSONObject show = response.getJSONArray("data").getJSONObject(0);
                        if (show.has("long_date")) {
                            result = show.getString("long_date");
                        }
                        if (show.has("location")) {
                            result += System.lineSeparator();
                            result += show.getString("location");
                            result += System.lineSeparator();
                        }
                    }
                }
            } else {
                result = "Error from web service";
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
        return result;
    }


    //**********DATA UPDATE RECEIVER************//

    private class DataUpdateReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DemoIntentService.RECEIVED_UPDATE)) {
                Log.d("DemoActivity", "hey I just got your broadcast!");
                mResultsButton.setEnabled(true);
                mNumberOfResults++;
                mResultsButton.setText("New results (" + mNumberOfResults + ")");
                mResultStrings.add(intent.getStringExtra(getString(R.string.keys_extra_results)));
            }
        }
    }

}
