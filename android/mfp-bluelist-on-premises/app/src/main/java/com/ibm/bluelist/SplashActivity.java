/**
 * Copyright 2015 IBM Corp.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.bluelist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.cloudant.sync.notifications.ReplicationCompleted;
import com.cloudant.sync.notifications.ReplicationErrored;
import com.cloudant.sync.replication.Replicator;
import com.google.common.eventbus.Subscribe;

/**
 * The {@code SplashActivity} is the splash dialog shown when the app is created for the first time.
 * During the splash, the BlueListApplication global variables are initialized and data is replicated from the remote database.
 */
public class SplashActivity extends Activity {
    private static final String TAG = SplashActivity.class.getCanonicalName();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_launch_screen);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Initialize application components
        Toast.makeText(getBaseContext(), "Initializing...", Toast.LENGTH_SHORT).show();
        BlueListApplication application = BlueListApplication.getInstance();
        application.initialize();

        // Register a listener for pull replication
        final Replicator pullReplicator = BlueListApplication.getInstance().getPullReplicator();
        pullReplicator.getEventBus().register(new Object() {

            // Launch MainActivity when replication completes
            @Subscribe
            public void complete(ReplicationCompleted event) {
                pullReplicator.getEventBus().unregister(this);
                Log.d(TAG, String.format("Pull replication complete. %d documents replicated.", event.documentsReplicated));

                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Subscribe
            public void error(ReplicationErrored event) {
                throw new RuntimeException(event.errorInfo.getException());
            }
        });

        // Start pull replication
        pullReplicator.start();
    }
}


