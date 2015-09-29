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
package com.ibm.bluelist;

import android.app.Application;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.util.Log;

import com.cloudant.http.HttpConnectionInterceptorContext;
import com.cloudant.http.HttpConnectionRequestInterceptor;
import com.cloudant.http.HttpConnectionResponseInterceptor;
import com.cloudant.sync.datastore.BasicDocumentRevision;
import com.cloudant.sync.datastore.ConflictException;
import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreManager;
import com.cloudant.sync.datastore.DatastoreNotCreatedException;
import com.cloudant.sync.datastore.DocumentBodyFactory;
import com.cloudant.sync.datastore.DocumentException;
import com.cloudant.sync.datastore.DocumentRevision;
import com.cloudant.sync.datastore.MutableDocumentRevision;
import com.cloudant.sync.datastore.encryption.AndroidKeyProvider;
import com.cloudant.sync.datastore.encryption.KeyProvider;
import com.cloudant.sync.datastore.encryption.NullKeyProvider;
import com.cloudant.sync.query.IndexManager;
import com.cloudant.sync.query.QueryResult;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorBuilder;
import com.worklight.wlclient.api.WLClient;
import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLResourceRequest;
import com.worklight.wlclient.api.WLResponse;
import com.worklight.wlclient.api.WLResponseListener;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class BlueListApplication extends Application {

    private static final String TAG = BlueListApplication.class.getCanonicalName();
    private static final String TODO_ITEM_INDEX_NAME = "todoItem_index";
    public static final String TODO_ITEM_NAME_KEY = "name";
    public static final String TODO_ITEM_PRIORITY_KEY = "priority";
    private static final String DATASTORE_DIR_NAME = "BlueListDatastores";
    private static final String BLUELIST_PROPERTIES_FILE = "bluelist.properties";
    final String PROP_NAME_BLUELIST_PROXY_ADAPTER_URL = "bluelistProxyAdapterURL";
    final String PROP_NAME_SEC_USERNAME = "secUserName";
    final String PROP_NAME_SEC_USERPW = "secUserPw";
    final String PROP_NAME_SEC_ADAPTERNAME = "secAdapterName";
    final String PROP_NAME_SEC_SCOPE = "secScope";
    private static final String PROP_NAME_PASSWORD = "password";
    private static final String KEY_PROVIDER_IDENTIFIER = "bluelist";

    private static final String CLOUDANT_ACCESS_KEY = "cloudant_access";
    private static final String PROTOCOL_KEY = "protocol";
    private static final String HOST_KEY = "host";
    private static final String PORT_KEY = "port";
    private static final String DATABASE_KEY = "database";
    private static final String SESSION_COOKIE_KEY = "sessionCookie";
    private static final String COOKIE_HEADER_KEY = "Cookie";
    private static final String DATATYPE_KEY = "@datatype";
    private static final String TODO_ITEM_DATATYPE = "TodoItem";

    private static final List<Map<String, String>> QUERY_SORT_BY_NAME_OPTION = new ArrayList<Map<String, String>>() {{
        Map<String, String> byName = new HashMap<String, String>() {{
            put(TODO_ITEM_NAME_KEY, "asc");
        }};
        add(byName);
    }};

    private static BlueListApplication instance;

    private DatastoreManager mDatastoreManager;
    private KeyProvider mKeyProvider;
    private Datastore mDatastore;
    private IndexManager mIndexManager;
    private Replicator mPullReplicator;
    private Replicator mPushReplicator;

    private URI mProxyUrl;
    private String mSecUsername;
    private String mSecUserPassword;
    private String mSecAdapterName;
    private String mSecScope;

    private String mRemoteDatabaseName;
    private URI mRemoteDatabaseURI;
    private String mSessionCookie;
    private String mKeyProviderPassword;
    private Typeface mTypeFace;

    /**
     * @return BlueListApplication instance
     */
    public static BlueListApplication getInstance() {
        return instance;
    }

    public Typeface getTypeFace() {
        return mTypeFace;
    }

    /**
     * Initializes global application variables.
     * <p/>
     * If not global variables are not configured, this method makes a call to the backend
     * application to fetch information for connecting to the remote database. It then configures
     * the Datastore and pull/push Replicators.
     */
    public void initialize() {
        if (mDatastore == null) {
            final CountDownLatch latch = new CountDownLatch(1);

            enroll(new Callback() {
                @Override
                public void success(Object object) {
                    configureDatastore();
                    configureReplicators();
                    latch.countDown();
                }

                @Override
                public void error(Throwable e) {
                    throw new RuntimeException(e);
                }
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        mTypeFace = Typeface.createFromAsset(getAssets(), "fonts/helvetica-neue-light.ttf");

        setBlueListProperties();

        // Register Challenge Handler

        // Create Challenge Handler and set properties
        BlueListChallengeHandler challengeHandler = new BlueListChallengeHandler(mSecScope);
        challengeHandler.UserName = mSecUsername;
        challengeHandler.UserPassword = mSecUserPassword;
        challengeHandler.AdapterName = mSecAdapterName;

        // Initialize WLClient and register challenge handler
        WLClient.createInstance(getApplicationContext()).registerChallengeHandler(challengeHandler);

        // Initialize DatastoreManager
        File path = getDir(DATASTORE_DIR_NAME, MODE_PRIVATE);
        mDatastoreManager = new DatastoreManager(path.getAbsolutePath());

        // Set KeyProvider to enable/disable encryption
        if (mKeyProviderPassword == null || mKeyProviderPassword.isEmpty()) {
            mKeyProvider = new NullKeyProvider();
        } else {
            mKeyProvider = new AndroidKeyProvider(this, mKeyProviderPassword, KEY_PROVIDER_IDENTIFIER);
        }
    }

    /**
     * Creates a TodoItem in the Datastore with "name": name and "priority": 0
     *
     * @param name - The name of the TodoItem
     * @return The DocumentRevision of the created TodoItem
     */
    public DocumentRevision addTodoItem(final String name) {
        MutableDocumentRevision todoItem = new MutableDocumentRevision();
        todoItem.body = DocumentBodyFactory.create(new HashMap<String, Object>() {{
            put(DATATYPE_KEY, TODO_ITEM_DATATYPE);
            put(TODO_ITEM_NAME_KEY, name);
            put(TODO_ITEM_PRIORITY_KEY, 0);
        }});

        try {
            return mDatastore.createDocumentFromRevision(todoItem);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a TodoItem in the Datastore with "name": name and "priority": 0
     *
     * @param newName     - The new name of the TodoItem
     * @param newPriority - The new priority of the TodoItem
     * @return The DocumentRevision of the created TodoItem
     */
    public DocumentRevision editTodoItem(BasicDocumentRevision documentRevision, final String newName, final Integer newPriority) {
        MutableDocumentRevision updateTodoItem = documentRevision.mutableCopy();
        Map<String, Object> body = documentRevision.getBody().asMap();

        if (newName != null) {
            body.put(TODO_ITEM_NAME_KEY, newName);
        }

        if (newPriority != null) {
            body.put(TODO_ITEM_PRIORITY_KEY, newPriority);
        }

        updateTodoItem.body = DocumentBodyFactory.create(body);

        try {
            return mDatastore.updateDocumentFromRevision(updateTodoItem);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes a TodoItem from the Datastore
     *
     * @param documentRevision - The TodoItem document revision
     * @return The deleted TodoItem DocumentRevision
     */
    public DocumentRevision removeTodoItem(DocumentRevision documentRevision) {
        try {
            return mDatastore.deleteDocumentFromRevision((BasicDocumentRevision) documentRevision);
        } catch (ConflictException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return All DocumentRevisions with "@datatype": "TodoItem" and "priority": priority
     */
    public QueryResult getTodoItemsByPriority(final int priority) {
        Map<String, Object> selector = new HashMap<String, Object>() {{
            Map<String, Object> datatypeEq = new HashMap<String, Object>() {{
                put("$eq", TODO_ITEM_DATATYPE);
            }};
            Map<String, Object> priorityEq = new HashMap<String, Object>() {{
                put("$eq", priority);
            }};

            put(DATATYPE_KEY, datatypeEq);
            put(TODO_ITEM_PRIORITY_KEY, priorityEq);
        }};

        return mIndexManager.find(selector, 0, 0, null, QUERY_SORT_BY_NAME_OPTION);
    }

    /**
     * @return All DocumentRevisions with "@datatype": "TodoItem"
     */
    public QueryResult getAllTodoItems() {
        Map<String, Object> selector = new HashMap<String, Object>() {{
            Map<String, Object> datatype = new HashMap<String, Object>() {{
                put("$eq", TODO_ITEM_DATATYPE);
            }};
            put(DATATYPE_KEY, datatype);
        }};

        return mIndexManager.find(selector, 0, 0, null, QUERY_SORT_BY_NAME_OPTION);
    }

    public Replicator getPullReplicator() {
        return mPullReplicator;
    }

    public Replicator getPushReplicator() {
        return mPushReplicator;
    }

    /**
     * Opens the datastore and ensures appropriate fields are indexed
     */
    @SuppressWarnings("Convert2Diamond")
    private void configureDatastore() {

        // Open the Datastore using the name of the remote database
        try {
            mDatastore = mDatastoreManager.openDatastore(mRemoteDatabaseName, mKeyProvider);
        } catch (DatastoreNotCreatedException e) {
            throw new RuntimeException(e);
        }

        mIndexManager = new IndexManager(mDatastore);
        List<Object> indexFields = new ArrayList<Object>();
        indexFields.add(DATATYPE_KEY);
        indexFields.add(TODO_ITEM_NAME_KEY);
        indexFields.add(TODO_ITEM_PRIORITY_KEY);
        mIndexManager.ensureIndexed(indexFields, TODO_ITEM_INDEX_NAME);
    }

    /**
     * Configures replicators with request/response interceptors
     */
    private void configureReplicators() {

        // Configure the request interceptor to use the session cookie
        HttpConnectionRequestInterceptor requestInterceptor = new HttpConnectionRequestInterceptor() {
            @Override
            public HttpConnectionInterceptorContext interceptRequest(HttpConnectionInterceptorContext context) {
                HttpURLConnection connection = context.connection.getConnection();

                // Set the last session cookie retrieved
                connection.setRequestProperty(COOKIE_HEADER_KEY, mSessionCookie);

                return context;
            }
        };

        // Configure response interceptor to refresh session cookie on when expired
        HttpConnectionResponseInterceptor responseInterceptor = new HttpConnectionResponseInterceptor() {
            @Override
            public HttpConnectionInterceptorContext interceptResponse(final HttpConnectionInterceptorContext context) {
                final HttpURLConnection connection = context.connection.getConnection();
                int responseCode = 0;
                try {
                    responseCode = connection.getResponseCode();
                } catch (IOException e) {
                    Log.e(TAG, "Error getting response code from connection", e);
                }

                // If the response failed due to a bad session cookie, obtain a new session cookie and try again.
                if (responseCode == 403) {
                    Log.d(TAG, "Attempting to refresh session cookie.");
                    final CountDownLatch latch = new CountDownLatch(1);
                    sessionCookie(new Callback() {
                        @Override
                        public void success(Object object) {
                            connection.setRequestProperty(COOKIE_HEADER_KEY, mSessionCookie);
                            context.replayRequest = true;
                            latch.countDown();
                        }

                        @Override
                        public void error(Throwable e) {
                            Log.e(TAG, "Response interceptor failed.", e);
                            context.replayRequest = false;
                            latch.countDown();
                        }
                    });
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Request to refresh session cookie interrupted.", e);
                    }
                }

                return context;
            }
        };

        mPullReplicator = ReplicatorBuilder.pull().from(mRemoteDatabaseURI).to(mDatastore).addRequestInterceptors(requestInterceptor).addResponseInterceptors(responseInterceptor).build();
        mPushReplicator = ReplicatorBuilder.push().from(mDatastore).to(mRemoteDatabaseURI).addRequestInterceptors(requestInterceptor).addResponseInterceptors(responseInterceptor).build();
    }

    /**
     * Closes Datastore and IndexManager
     */
    public void tearDown() {
        mDatastore.close();
        mIndexManager.close();
        mDatastore = null;
        mIndexManager = null;
        mPullReplicator = null;
        mPushReplicator = null;
    }

    /**
     * @return - Properties from bluelist.properties file located in assets folder
     */
    private Properties loadPropertiesFile() {
        Properties props = new java.util.Properties();
        AssetManager assetManager = getAssets();
        try {
            props.load(assetManager.open(BLUELIST_PROPERTIES_FILE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Log.i(TAG, "Found configuration file: " + BLUELIST_PROPERTIES_FILE);

        return props;
    }

    /**
     * Ensures valid properties are set from bluelist.properties file
     */
    private void setBlueListProperties() {
        Properties props = loadPropertiesFile();

        try {
            mProxyUrl = new URI(props.getProperty(PROP_NAME_BLUELIST_PROXY_ADAPTER_URL));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        mSecUsername = props.getProperty(PROP_NAME_SEC_USERNAME);
        mSecUserPassword = props.getProperty(PROP_NAME_SEC_USERPW);
        mSecAdapterName = props.getProperty(PROP_NAME_SEC_ADAPTERNAME);
        mSecScope = props.getProperty(PROP_NAME_SEC_SCOPE);

        mKeyProviderPassword = props.getProperty(PROP_NAME_PASSWORD);

        if (mProxyUrl == null
                || mSecUsername == null || mSecUsername.isEmpty()
                || mSecUserPassword == null || mSecUserPassword.isEmpty()
                || mSecAdapterName == null || mSecAdapterName.isEmpty()
                || mSecScope == null || mSecScope.isEmpty()) {
            throw new RuntimeException(
                    String.format("%s file is not configured correctly.%n%s: %s%n%s: %s%n%s: %s%n%s: %s%n%s: %s%n%s: %s",
                            BLUELIST_PROPERTIES_FILE,
                            PROP_NAME_BLUELIST_PROXY_ADAPTER_URL, mProxyUrl,
                            PROP_NAME_SEC_USERNAME, mSecUsername,
                            PROP_NAME_SEC_USERPW, mSecUserPassword,
                            PROP_NAME_SEC_ADAPTERNAME, mSecAdapterName,
                            PROP_NAME_SEC_SCOPE, mSecScope,
                            PROP_NAME_PASSWORD, mKeyProviderPassword));
        }

        Log.i(TAG, "Successfully loaded all values from " + BLUELIST_PROPERTIES_FILE + " file");
    }

    /**
     * Calls backend application route enroll
     * <p/>
     * Response payload includes the required information for accessing the remote Cloudant database
     *
     * @param callback - The callback methods to invoke once the enroll function is complete
     */
    private void enroll(final Callback callback) {

        URI requestUri = null;
        try {
            requestUri = new URI(mProxyUrl + "/enroll");
        } catch (URISyntaxException e) {
            callback.error(new Exception(String.format("Could not enroll user. %s", e.getMessage())));
        }

        final WLResourceRequest request = new WLResourceRequest(requestUri, WLResourceRequest.PUT);
        request.send(new WLResponseListener() {

            @Override
            public void onSuccess(WLResponse response) {
                if (response.getStatus() != 200) {
                    callback.error(new Exception(String.format("Could not enroll user. %s", response.toString())));
                } else {

                    try {
                        JSONObject json = new JSONObject(response.getResponseText());
                        JSONObject cloudantAccess = json.getJSONObject(CLOUDANT_ACCESS_KEY);
                        String protocol = cloudantAccess.getString(PROTOCOL_KEY);
                        String host = cloudantAccess.getString(HOST_KEY);
                        int port = cloudantAccess.getInt(PORT_KEY);
                        mRemoteDatabaseName = json.getString(DATABASE_KEY);
                        mRemoteDatabaseURI = new URI(protocol + "://" + host + ":" + port + "/" + mRemoteDatabaseName);

                        mSessionCookie = json.getString(SESSION_COOKIE_KEY);
                    } catch (Exception e) {
                        callback.error(e);
                    }
                    callback.success(response);
                }
            }

            @Override
            public void onFailure(WLFailResponse failResponse) {
                if (failResponse.getErrorMsg() != null) {
                    callback.error(new Exception(String.format("Could not enroll user. %s", failResponse.getErrorMsg())));
                } else if (failResponse.getResponseJSON() != null) {
                    callback.error(new Exception(String.format("Could not enroll user. Error info:%n%s", failResponse.getResponseJSON().toString())));
                } else {
                    callback.error(new Exception("Could not enroll user. Reason unknown."));
                }
            }
        });
    }

    /**
     * Calls backend application route sessioncookie
     * <p/>
     * Response payload includes the required information for accessing the remote Cloudant database
     *
     * @param callback - The callback methods to invoke once the sessioncookie function is complete
     */
    private void sessionCookie(final Callback callback) {

        URI requestUri = null;
        try {
            requestUri = new URI(mProxyUrl + "/sessioncookie");
        } catch (URISyntaxException e) {
            callback.error(new Exception(String.format("Could not get session cookie for user. %s", e.getMessage())));
        }

        final WLResourceRequest request = new WLResourceRequest(requestUri, WLResourceRequest.POST);
        request.send(new WLResponseListener() {

            @Override
            public void onSuccess(WLResponse response) {
                if (response.getStatus() != 200) {
                    callback.error(new Exception(String.format("Could not get session cookie for user. %s", response.toString())));
                } else {
                    try {
                        JSONObject json = new JSONObject(response.getResponseText());
                        mSessionCookie = json.getString(SESSION_COOKIE_KEY);
                    } catch (Exception e) {
                        callback.error(e);
                    }
                    callback.success(response);
                }
            }

            @Override
            public void onFailure(WLFailResponse failResponse) {
                if (failResponse.getErrorMsg() != null) {
                    callback.error(new Exception(String.format("Could not get session cookie for user. %s", failResponse.getErrorMsg())));
                } else if (failResponse.getResponseJSON() != null) {
                    callback.error(new Exception(String.format("Could not get session cookie for user. Error info:%n%s", failResponse.getResponseJSON().toString())));
                } else {
                    callback.error(new Exception("Could not get session cookie for user. Reason unknown."));
                }
            }
        });
    }

    interface Callback {
        void success(Object object);

        void error(Throwable e);
    }
}