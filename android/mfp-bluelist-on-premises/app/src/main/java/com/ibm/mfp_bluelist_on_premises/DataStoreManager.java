package com.ibm.mfp_bluelist_on_premises;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.cloudant.sync.notifications.ReplicationCompleted;
import com.cloudant.sync.notifications.ReplicationErrored;
import com.cloudant.sync.replication.ErrorInfo;
import com.cloudant.sync.replication.PullReplication;
import com.cloudant.sync.replication.PushReplication;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorFactory;
import com.cloudant.toolkit.IndexField;
import com.cloudant.toolkit.Store;
import com.cloudant.toolkit.mapper.DataObjectMapper;
import com.cloudant.toolkit.query.CloudantQuery;
import com.google.common.eventbus.Subscribe;
import com.ibm.imf.data.DataManager;
import com.worklight.wlclient.api.WLClient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by drcariel on 3/12/2015.
 */
public class DataStoreManager {
    private static DataStoreManager instance = null;

    /**
     * Default property values used if Bluelist properties not set. DO I need this? TODO: Move some code else where, maybe move all data interactions tpo be separate from UI
     */
    private static String DEFAULT_CLOUDANT_PROXY_URL = "http://10.0.2.2:9080/imfdata";  //"http://imfdata02.rtp.raleigh.ibm.com:10080/imfdata";
    private static String DEFAULT_DBName = "todosdb";
    private static String DEFAULT_NAME_SEC_USERNAME = "james";
    private static String DEFAULT_NAME_SEC_USERPW = "42";
    private static String DEFAULT_NAME_SEC_ADAPTERNAME = "CloudantAuthenticationAdapter";
    private static String DEFAULT_NAME_SEC_SCOPE = "cloudant";

    /**
     * Values set from Bluelist properties file.
     */
    private static String CLOUDANT_PROXY_URL;  // e.g. "http://imfdata02.rtp.raleigh.ibm.com:10080/imfdata";
    private static String DBName;              // name of the database with the items documents

    /**
     * Bluelist properties
     */
    private static final String PROPS_FILE = "bluelist.properties";
    private static final String PROP_NAME_CLOUDANT_PROXY_URL = "cloudantProxyURL";
    private static final String PROP_NAME_TODOS_DATABASE_NAME = "todosDatabaseName";
    private static final String PROP_NAME_SEC_USERNAME = "secUserName";
    private static final String PROP_NAME_SEC_USERPW = "secUserPw";
    private static final String PROP_NAME_SEC_ADAPTERNAME = "secAdapterName";
    private static final String PROP_NAME_SEC_SCOPE = "secScope";

    private static final String CLASS_NAME = DataStoreManager.class.getSimpleName();
    private static final String IndexName = "todosIndex";
    ArrayList<Item> itemList;
    Store todosStore;
    Store remoteStore;
    Store localStore;
    DataManager manager;
    WLClient client;
    SampleChallengeHandler sampleChallengeHandler;

    // Should I make the user supply a context everytime they want to call this?
    public static DataStoreManager getInstance(Context context) {
        if(instance == null) {
            instance = new DataStoreManager(context);
        }
        return instance;
    }

    protected DataStoreManager(Context context){

        itemList = new ArrayList<Item>();

        Properties props = new java.util.Properties();
        //Context context = getApplicationContext();
        try {
            AssetManager assetManager = context.getAssets();
            props.load(assetManager.open(PROPS_FILE));
            Log.i(CLASS_NAME, "Found configuration file: " + PROPS_FILE);
            // proxy URL
            CLOUDANT_PROXY_URL = props.getProperty(PROP_NAME_CLOUDANT_PROXY_URL);
            if (CLOUDANT_PROXY_URL==null || CLOUDANT_PROXY_URL.isEmpty() || CLOUDANT_PROXY_URL.equalsIgnoreCase("")){
                CLOUDANT_PROXY_URL = DEFAULT_CLOUDANT_PROXY_URL;
            }

            // DB name
            DBName = props.getProperty(PROP_NAME_TODOS_DATABASE_NAME);
            if (DBName==null || DBName.isEmpty() || DBName.equalsIgnoreCase("")){
                DBName = DEFAULT_DBName;
            }

            // sample challenge handler properties
            String secUsername = props.getProperty(PROP_NAME_SEC_USERNAME);
            if (secUsername==null || secUsername.isEmpty() || secUsername.equalsIgnoreCase("")){
                secUsername = DEFAULT_NAME_SEC_USERNAME;
            }
            String secUserPassword = props.getProperty(PROP_NAME_SEC_USERPW);
            if (secUserPassword==null || secUserPassword.isEmpty() || secUserPassword.equalsIgnoreCase("")){
                secUserPassword = DEFAULT_NAME_SEC_USERPW;
            }
            String secAdapaterName = props.getProperty(PROP_NAME_SEC_ADAPTERNAME);
            if (secAdapaterName==null || secAdapaterName.isEmpty() || secAdapaterName.equalsIgnoreCase("")){
                secAdapaterName = DEFAULT_NAME_SEC_ADAPTERNAME;
            }
            String secScope = props.getProperty(PROP_NAME_SEC_SCOPE);
            if (secScope==null || secScope.isEmpty() || secScope.equalsIgnoreCase("")){
                secScope = DEFAULT_NAME_SEC_SCOPE;
            }
            sampleChallengeHandler = new SampleChallengeHandler(secScope);
            sampleChallengeHandler.UserName = secUsername;
            sampleChallengeHandler.UserPassword = secUserPassword;
            sampleChallengeHandler.AdapterName = secAdapaterName;

            Log.i(CLASS_NAME, "Cloudant Proxy URL value: " + CLOUDANT_PROXY_URL);
            Log.i(CLASS_NAME, "Database name value: " + DBName);
            Log.i(CLASS_NAME, "CH username: " + secUsername);
            Log.i(CLASS_NAME, "CH user password: " + secUserPassword);
            Log.i(CLASS_NAME, "CH adapter name: " + secAdapaterName);
            Log.i(CLASS_NAME, "CH scope: " + secScope);
        } catch (FileNotFoundException e) {
            Log.e(CLASS_NAME, "The bluelist.properties file was not found.", e);
        } catch (IOException e) {
            Log.e(CLASS_NAME,
                    "The bluelist.properties file could not be read properly.", e);
        }

        // Initialize DataManager and WLClient
        try {
            manager = DataManager.initialize(context, new URL(CLOUDANT_PROXY_URL));
            client = WLClient.createInstance(context);
            client.registerChallengeHandler(sampleChallengeHandler);
        } catch (MalformedURLException e) {
            Log.e(CLASS_NAME,
                    "The Cloudant proxy URL was invalid.", e);
        }

        // Create the local store
        try {
            final Task<Store> localtask = manager.localStore(DBName);
            localtask.waitForCompletion();

            if (localtask.isFaulted()) {
                System.out.println("Failed to create localStore DB name: " + DBName + "\nError: " + localtask.getError().getLocalizedMessage());
                throw localtask.getError();
            } else {
                localStore = localtask.getResult();
            }
        } catch (Exception e) {
            Log.e(CLASS_NAME,
                    "DataManager failed to create a local datastore.", e);
        }

        // Create the remote store
        try {
            final Task<Store> task = manager.remoteStore(DBName);
            task.waitForCompletion();

            if (task.isFaulted()) {
                System.out.println("Failed to create remote DB name: " + DBName + "\nError: " + task.getError().getLocalizedMessage());
                throw task.getError();
            } else {
                remoteStore = task.getResult();
                final Task<Boolean> task2 = manager.setCurrentUserPermissions("admins", DBName);
                task2.waitForCompletion();

                if (task2.isFaulted()) {
                    System.out.println("Failed to set permissions on DB name: " + DBName + "\nError: " + task2.getError().getLocalizedMessage());
                    throw task2.getError();
                }
            }
        } catch (Exception e) {
            Log.e(CLASS_NAME,
                    "DataManager failed to create a remote datastore.", e);
        }

        todosStore = getStore();

        // Set the data object mapper
        todosStore.setMapper(new DataObjectMapper());
        try {
            todosStore.getMapper().setDataTypeForClassName("TodoItem", Item.class.getCanonicalName());//TODO THIS IS CAUSING THE ERROR?????????????????????????
        } catch (Exception e) {
            Log.e(CLASS_NAME, "Error setting data type for class", e);
        }

        List<IndexField> indexFields = new ArrayList<IndexField>();
        indexFields.add(new IndexField("@datatype"));

        Task t = todosStore.createIndex(IndexName, indexFields);

        try {
            t.waitForCompletion();
        } catch (InterruptedException e2) {
            Log.e(CLASS_NAME, "Interrupted waiting for creation of index", e2);
        }

        if (t.isFaulted()) {
            Log.e(CLASS_NAME, "Error creating index", t.getError());
        }

        sync(true);  // 'true': wait for DBs to synchronize
    }
    public Store getStore() {
        return localStore;
    }

    public void setItemList() {


        try {
            Map<String, Object> queryJSON = new HashMap<String, Object>();
            Map<String, Object> selector = new HashMap<String, Object>();
            Map<String, Object> equalityOp = new HashMap<String, Object>();
            equalityOp.put("$eq", "TodoItem");
            selector.put("@datatype", equalityOp);
            queryJSON.put("selector", selector);

            CloudantQuery query = new CloudantQuery(queryJSON);

            // Query all the Item objects from the server.
            localStore.performQuery(query).continueWith(new Continuation<List, Void>() {

                // This separate thread is giving me null pointers in my UI
                @Override
                public Void then(Task<List> task) throws Exception {
                    final List<Item> objects = task.getResult();
                    // Log if the find was cancelled.
                    if (task.isCancelled()){
                        Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");
                    }
                    // Log error message, if the find task fails.
                    else if (task.isFaulted()) {
                        Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                    }


                    // If the result succeeds, load the list.
                    else {
                        // Clear local itemList.
                        itemList.clear();
                        // We'll be reordering and repopulating from DataService.
                        for(Item item:objects) {
                            itemList.add((Item) item);
                        }
                        sortItems(itemList);
                    }
                    return null;
                }
            },Task.UI_THREAD_EXECUTOR);

        }  catch (Exception error) {
            Log.e(CLASS_NAME, "Exception : " + error.getMessage());
        }

    }

    public ArrayList<Item> getItemList(){
        return itemList;
    }

    private void sortItems(List<Item> theList) {
        // Sort collection by case insensitive alphabetical order.
        Collections.sort(theList, new Comparator<Item>() {
            public int compare(Item lhs,
                               Item rhs) {
                String lhsName = lhs.getName();
                String rhsName = rhs.getName();
                return lhsName.compareToIgnoreCase(rhsName);
            }
        });
    }

    private boolean doPullReplication(boolean waitForSync) {
        try{
            //Toast.makeText(MainActivity.this, "Pulling Items From Cloudant", Toast.LENGTH_SHORT).show();
            // create one-way replication task
            Task<PullReplication> pullTask = manager.pullReplicationForStore(DBName);
            pullTask.waitForCompletion();
            if (pullTask.isFaulted()){
                //Toast.makeText(MainActivity.this, "Pull replication error: "+ pullTask.getError(), Toast.LENGTH_LONG).show();
                Log.e(CLASS_NAME, "Pull replication error", pullTask.getError());
                return false;
            }
            else {
                PullReplication pull = pullTask.getResult();
                Replicator replicator = ReplicatorFactory.oneway(pull);

                if (waitForSync) {
                    // Use a CountDownLatch to provide a lightweight way to wait for completion
                    CountDownLatch latch = new CountDownLatch(1);
                    Listener listener = new Listener(latch);
                    replicator.getEventBus().register(listener);
                    replicator.start();
                    latch.await();
                    replicator.getEventBus().unregister(listener);
                    if (replicator.getState() != Replicator.State.COMPLETE) {
                        //Toast.makeText(MainActivity.this, "Pull replication error: "+ listener.error.toString(), Toast.LENGTH_LONG).show();
                        Log.e(CLASS_NAME, "Pull replication failed.  Error replicating to local.");
                        Log.e(CLASS_NAME, listener.error.toString());
                    }
                }
                else {
                    replicator.start();
                }
            }
        } catch (Exception e) {
            // replication failed
            Log.e(CLASS_NAME, "Pull replication error", e);
            return false;
        }
        return true;
    }

    private boolean doPushReplication(boolean waitForSync) {
        try{
            //Toast.makeText(MainActivity.this, "Pushing Items to Cloudant", Toast.LENGTH_SHORT).show();
            // create one-way replication task
            Task<PushReplication> pushTask = manager.pushReplicationForStore(DBName);
            pushTask.waitForCompletion();
            if (pushTask.isFaulted()){
                Log.e(CLASS_NAME, "Push replication error", pushTask.getError());
                //Toast.makeText(MainActivity.this, "Push replication error: "+ pushTask.getError(), Toast.LENGTH_LONG).show();
                return false;
            }
            else {
                PushReplication push = pushTask.getResult();
                Replicator replicator = ReplicatorFactory.oneway(push);

                if (waitForSync) {
                    // Use a CountDownLatch to provide a lightweight way to wait for completion
                    CountDownLatch latch = new CountDownLatch(1);
                    Listener listener = new Listener(latch);
                    replicator.getEventBus().register(listener);
                    replicator.start();
                    latch.await();
                    replicator.getEventBus().unregister(listener);
                    if (replicator.getState() != Replicator.State.COMPLETE) {
                        //Toast.makeText(MainActivity.this, "Push replication error: "+ listener.error.toString(), Toast.LENGTH_LONG).show();
                        Log.e(CLASS_NAME, "Push replication failed.  Error replicating to remote.");
                        Log.e(CLASS_NAME, listener.error.toString());
                    }
                }
                else {
                    replicator.start();
                }
            }
        } catch (Exception e) {
            // replication failed
            Log.e(CLASS_NAME, "Push replication error", e);
            return false;
        }
        return true;
    }

    /**
     * Synchronizes local DB with the remote DB
     * @param waitForSync if 'true', method waits for sync to complete before returning
     */
    public void sync(boolean waitForSync) {
        boolean repSuccess = false;
        //Toast.makeText(MainActivity.this, "Database replication started", Toast.LENGTH_LONG).show();
        Log.i(CLASS_NAME, "Database replication started");
        repSuccess = doPullReplication(waitForSync);
        if (repSuccess){

            repSuccess = doPushReplication(waitForSync);

        }
        if (repSuccess) {
            Log.i(CLASS_NAME, "Database replication completed successfully");
            //Toast.makeText(MainActivity, "Database replication completed successfully", Toast.LENGTH_SHORT).show();
            setItemList();

        } else {
            //Toast.makeText(MainActivity.this, "Database replication was not successful", Toast.LENGTH_SHORT).show();
            Log.w(CLASS_NAME, "Database replication was not successful");
        }

    }

    public Store getTodosStore(){return todosStore;}

    /**
     * A {@code ReplicationListener} that sets a latch when it's told the
     * replication has finished.
     */
    private class Listener {

        private final CountDownLatch latch;
        public ErrorInfo error = null;

        Listener(CountDownLatch latch) {
            this.latch = latch;
        }

        @Subscribe
        public void complete(ReplicationCompleted event) {
            latch.countDown();
        }

        @Subscribe
        public void error(ReplicationErrored event) {
            this.error = event.errorInfo;
            latch.countDown();
        }
    }

}
