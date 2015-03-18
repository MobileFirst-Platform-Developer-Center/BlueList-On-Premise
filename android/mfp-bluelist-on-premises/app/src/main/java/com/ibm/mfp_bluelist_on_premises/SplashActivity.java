package com.ibm.mfp_bluelist_on_premises;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

/**
 * The {@code SplashActivity} is the splash dialog shown when the app is created for the first time.
 * During the splash, the DataStoreManager is created in a background task, which verifies authentication through the MFP server and connects to the remote datastore.
 */
public class SplashActivity extends Activity {

    Activity splash;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_launch_screen);

        splash = this;
        // Execute async task
        new AsyncLoadDataManager().execute();

    }

    /**
     * Async task that creates DataStoreManager in a background thread.
     * When complete, the MainActivity is created and the splash is dismissed.
     */
    private class AsyncLoadDataManager extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute(){

            Toast.makeText(getBaseContext(), "LOADING DATA", Toast.LENGTH_SHORT).show();

        }

        @Override
        protected Void doInBackground(Void... voids){

            DataStoreManager.getInstance(getApplicationContext(), splash);

            return null;
        }

        @Override
        protected void onPostExecute(Void params){

            // dismiss the dialog
            // launch the Main activity
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);

            // close this activity
            finish();
        }

    }

}
