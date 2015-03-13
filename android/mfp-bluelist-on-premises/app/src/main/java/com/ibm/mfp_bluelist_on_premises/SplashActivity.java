package com.ibm.mfp_bluelist_on_premises;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Created by drcariel on 3/12/2015.
 */
public class SplashActivity extends Activity {

    Activity splash;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_launch_screen);

        splash = this;

        new AsyncLoadDataManager().execute();

    }

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
