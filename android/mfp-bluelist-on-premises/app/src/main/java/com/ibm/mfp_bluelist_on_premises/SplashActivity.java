/**
 * Copyright 2015 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
