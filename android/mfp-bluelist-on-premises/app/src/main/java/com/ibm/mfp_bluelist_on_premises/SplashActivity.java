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
        // set the content view for your splash screen you defined in an xml file
        setContentView(R.layout.activity_launch_screen);

        splash = this;

        // perform other stuff you need to do

        // execute your xml news feed loader
        new AsyncLoadDataManager().execute();

    }

    private class AsyncLoadDataManager extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute(){
            // show your progress dialog
            Toast.makeText(getBaseContext(), "LOADING DATA", Toast.LENGTH_SHORT).show();

        }

        @Override
        protected Void doInBackground(Void... voids){
            // load your xml feed asynchronously
            DataStoreManager.getInstance(getApplicationContext(), splash);
            //Toast.makeText(getBaseContext(), "WORKING", Toast.LENGTH_LONG).show();
            return null;
        }

        @Override
        protected void onPostExecute(Void params){
            Toast.makeText(getBaseContext(), "DONE LOADING", Toast.LENGTH_SHORT).show();
            // dismiss your dialog
            // launch the Main activity
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);



            // close this activity
            finish();
        }

    }

}
