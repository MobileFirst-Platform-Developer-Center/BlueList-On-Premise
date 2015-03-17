package com.ibm.mfp_bluelist_on_premises;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.widget.Toast;

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
 * A {@code DataStoreManager} is used to manage the local and remote data stores.
 * On creation, this singleton sets the object mapper and the WLClient along with the Data Manager.
 * At the end of creation, objects are all pulled from the remote store to the local store.
 * Each object is then formed into a TodoItem and saved in an ArrayList.
 */
public class DataStoreManager {
    private static DataStoreManager instance = null;

    /**
     * Default property values used if Bluelist.properties is not set. DO I need this?
     */
    private static String DEFAULT_CLOUDANT_PROXY_URL = "http://10.0.2.2:9080/imfdata";
    private static String DEFAULT_DBName = "todosdb";
    private static String DEFAULT_NAME_SEC_USERNAME = "james";
    private static String DEFAULT_NAME_SEC_USERPW = "42";
    private static String DEFAULT_NAME_SEC_ADAPTERNAME = "CloudantAuthenticationAdapter";
    private static String DEFAULT_NAME_SEC_SCOPE = "cloudant";

    /**
     * Values set from Bluelist.properties file.
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

    ArrayList<TodoItem> todoItemList;
    Store todosStore;
    Store remoteStore;
    Store localStore;
    DataManager manager;
    WLClient client;
    BlueListChallengeHandler blueListChallengeHandler;
    Context context;
    Activity activity;

    protected DataStoreManager(Context context, Activity activity){

        todoItemList = new ArrayList<TodoItem>();

        this.activity = activity;

        this.context = context;

        Properties props = new java.util.Properties();
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

            blueListChallengeHandler = new BlueListChallengeHandler(secScope);
            blueListChallengeHandler.UserName = secUsername;
            blueListChallengeHandler.UserPassword = secUserPassword;
            blueListChallengeHandler.AdapterName = secAdapaterName;

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
            client.registerChallengeHandler(blueListChallengeHandler);
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
            todosStore.getMapper().setDataTypeForClassName("TodoItem", TodoItem.class.getCanonicalName());
        } catch (Exception e) {
            Log.e(CLASS_NAME, "Error setting data type for class", e);
        }

        List<IndexField> indexFields = new ArrayList<IndexField>();
        // TODO: Ask about this indexing
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

        doPullReplication(true);

        setItemList();

    }

    /**
     * When needed, the DataStoreManager can be grabbed using DataStoreManager.getInstance
     * Only one should ever be created so if one does not exist, it will be created.
     * @param context needed for WLClient and DataManager initialization and to find props files
     * @param activity needed to handle Toasts in pull and push replication
     * @return instance the DataStoreManager Singleton
     */
    public static DataStoreManager getInstance(Context context, Activity activity) {
        if(instance == null) {
            instance = new DataStoreManager(context, activity);
        }
        return instance;
    }

    /**
     *
     * @param context
     */
    public void setContext(Context context){
        this.context = context;
    }

    /**
     *
     * @param activity needed for change from splash screen to MainActivity
     */
    public void setActivity(Activity activity){
        this.activity = activity;
    }

    /**
     * Grabs local Store
     * @return localStore
     */
    public Store getStore() {
        return localStore;
    }

    /**
     * Converts local store into and ArrayList of TodoItem objects
     */
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

                @Override
                public Void then(Task<List> task) throws Exception {
                    final List<TodoItem> objects = task.getResult();
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
                        todoItemList.clear();
                        // We'll be reordering and repopulating from DataService.
                        for(TodoItem todoItem :objects) {
                            todoItemList.add((TodoItem) todoItem);
                        }
                        sortItems(todoItemList);
                    }
                    return null;
                }
            },Task.UI_THREAD_EXECUTOR);

        }  catch (Exception error) {
            Log.e(CLASS_NAME, "Exception : " + error.getMessage());
        }

    }

    /**
     * Returns created ArrayList from setItemList
     * @return todoItemList
     */
    public ArrayList<TodoItem> getTodoItemList(){
        return todoItemList;
    }

    /**
     * Sorts the list by case insensitive alphabetical order.
     * @param theList is the list to be sorted.
     */
    public void sortItems(List<TodoItem> theList) {
        Collections.sort(theList, new Comparator<TodoItem>() {
            public int compare(TodoItem lhs,
                               TodoItem rhs) {
                String lhsName = lhs.getName();
                String rhsName = rhs.getName();
                return lhsName.compareToIgnoreCase(rhsName);
            }
        });
    }

    /**
     * Pulls the objects from the remote store to the local store.
     * @param waitForSync if 'true', method waits for sync to complete before returning.
     * @return 'true' for success and 'false' if failed.
     */
    public boolean doPullReplication(boolean waitForSync) {
        try{
            // Had to create a new Runnable on UiThread in order for Toast to show up in background thread
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Pulling Items from Cloudant remote", Toast.LENGTH_LONG).show();
                }
            });

            // create one-way replication task
            Task<PullReplication> pullTask = manager.pullReplicationForStore(DBName);
            pullTask.waitForCompletion();
            if (pullTask.isFaulted()){

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

                        Log.e(CLASS_NAME, "Pull replication failed.  Error replicating to local.");
                        Log.e(CLASS_NAME, listener.error.toString());
                    }
                }
                else {
                    replicator.start();
                }
                // Create Array List from pulled down objects
                setItemList();
            }
        } catch (Exception e) {
            // Had to create a new Runnable on UiThread in order for Toast to show up in background thread
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Error replicating with Cloudant", Toast.LENGTH_SHORT).show();
                }
            });
            // replication failed
            Log.e(CLASS_NAME, "Pull replication error", e);
            return false;
        }
        return true;
    }

    /**
     * Pushes the objects to the remote store from the local store.
     * @param waitForSync if 'true', method waits for sync to complete before returning.
     * @return 'true' for success and 'false' if failed.
     */
    private boolean doPushReplication(boolean waitForSync) {
        try{
            // Had to create a new Runnable on UiThread in order for Toast to show up in background thread
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Pushing Items to Cloudant remote", Toast.LENGTH_LONG).show();
                }
            });

            // create one-way replication task
            Task<PushReplication> pushTask = manager.pushReplicationForStore(DBName);
            pushTask.waitForCompletion();
            if (pushTask.isFaulted()){
                Log.e(CLASS_NAME, "Push replication error", pushTask.getError());

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

                        Log.e(CLASS_NAME, "Push replication failed.  Error replicating to remote.");
                        Log.e(CLASS_NAME, listener.error.toString());
                    }

                }
                else {
                    replicator.start();

                }
            }
        } catch (Exception e) {
            // Had to create a new Runnable on UiThread in order for Toast to show up in background thread
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Error replicating with Cloudant", Toast.LENGTH_LONG).show();
                }
            });
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
        Log.i(CLASS_NAME, "Database replication started");
        repSuccess = doPullReplication(waitForSync);
        if (repSuccess){
            repSuccess = doPushReplication(waitForSync);
        }
        if (repSuccess) {
            setItemList();
            Log.i(CLASS_NAME, "Database replication completed successfully");

        } else {

            Log.w(CLASS_NAME, "Database replication was not successful");
        }

    }

    /**
     *
     * @return
     */
    public Store getTodosStore(){return todosStore;}

    /**
     * A {@code ReplicationListener} that sets a latch when it's told the
     * replication has finished.
     */
    private class Listener {

        private final CountDownLatch latch;
        public ErrorInfo error = null;

        /**
         *
         * @param latch
         */
        Listener(CountDownLatch latch) {
            this.latch = latch;
        }

        /**
         *
         * @param event
         */
        @Subscribe
        public void complete(ReplicationCompleted event) {
            latch.countDown();
        }

        /**
         *
         * @param event
         */
        @Subscribe
        public void error(ReplicationErrored event) {
            this.error = event.errorInfo;
            latch.countDown();
        }
    }
}
