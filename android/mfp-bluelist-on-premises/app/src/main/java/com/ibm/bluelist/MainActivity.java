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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.cloudant.sync.datastore.BasicDocumentRevision;
import com.cloudant.sync.datastore.DocumentRevision;
import com.cloudant.sync.notifications.ReplicationCompleted;
import com.cloudant.sync.notifications.ReplicationErrored;
import com.cloudant.sync.query.QueryResult;
import com.cloudant.sync.replication.Replicator;
import com.google.common.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ibm.bluelist.BlueListApplication.TODO_ITEM_NAME_KEY;
import static com.ibm.bluelist.BlueListApplication.TODO_ITEM_PRIORITY_KEY;


/**
 * The {@code MainActivity} is the primary visual activity shown when the app is being interacted with. Most of the code is UI and visuals.
 */
public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ListView mListView; // Main ListView
    private List<DocumentRevision> mTodoItemList; // The list of TodoItems
    private TodoItemAdapter mTodoItemAdapter; // Adapter for bridging the list of TodoItems with the ListView

    private SwipeRefreshLayout mSwipeLayout; // Swipe refresh for data replication

    private ActionBar mActionBar; // Action bar for navigating between tabs

    private BlueListApplication mApplication; // Application holds global variables for working with data

    private boolean push;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mApplication = BlueListApplication.getInstance();

        int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
        TextView titleView = (TextView) findViewById(titleId);
        titleView.setTypeface(mApplication.getTypeFace());

        initListView();
        initSwipeRefresh();
        initTabs();

        //TODO this needs to go in BlueListApplication when implemented
        push = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.push_switch, menu);

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        // If MainActivity is resumed from a stopped state, ensure the Datastore and pull/push Replicators are configured
        mApplication.initialize();

        // load list of TodoItems
        loadList();
    }

    @Override
    public void onStop() {

        // When MainActvity is stopped ensure Datastore is closed and other global variables are torn down
        mApplication.tearDown();
        super.onStop();
    }

    @SuppressWarnings("Convert2Diamond")
    private void initListView() {
        // Get MainActivity's ListView
        mListView = (ListView) findViewById(R.id.listView);

        // Init array to hold TodoItem DocumentRevisions
        mTodoItemList = new ArrayList<DocumentRevision>();

        // Set ListView adapter for displaying TodoItems
        mTodoItemAdapter = new TodoItemAdapter(getBaseContext(), mTodoItemList);
        mListView.setAdapter(mTodoItemAdapter);

        // Set long click listener for TodoItems to be deleted
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(android.widget.AdapterView<?> parent, View view, int position, long id) {

                // Grab TodoItem to delete from current showing list
                DocumentRevision todoItem = mTodoItemList.get(position);

                // Delete TodoItem from Datastore
                DocumentRevision deletedRevision = mApplication.removeTodoItem(todoItem);

                loadList();

                // Callback is consumed if the revision is successfully deleted
                return ((BasicDocumentRevision) deletedRevision).isDeleted();
            }
        });
    }

    private void initSwipeRefresh() {

        // Grab Swipe Refresh Layout
        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);

        // Set color scheme for swipe refresh listener
        mSwipeLayout.setColorSchemeResources(R.color.white, R.color.black, R.color.light_blue);

        // Set swipe refresh listener for sync on pull-down of the list
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                sync();
            }
        });

        // Could not get getResources().getColor to work, I believe there is a bug in that code, but this works
        mSwipeLayout.setProgressBackgroundColorSchemeResource(R.color.blue);
    }

    private void initTabs() {
        mActionBar = getActionBar();

        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ActionBar.TabListener listener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                loadList();
            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

            }
        };

        // Configure All priority tab
        ActionBar.Tab allTab = mActionBar.newTab();
        TextView allTextView = (TextView) getLayoutInflater().inflate(R.layout.tab_title, null);
        allTextView.setText("All");
        allTextView.setTypeface(mApplication.getTypeFace());
        allTab.setCustomView(allTextView);
        allTab.setTabListener(listener);

        // Configure Medium priority tab
        ActionBar.Tab mediumTab = mActionBar.newTab();
        TextView mediumTextView = (TextView) getLayoutInflater().inflate(R.layout.tab_title, null);
        mediumTextView.setText("Medium");
        mediumTextView.setTypeface(mApplication.getTypeFace());
        mediumTab.setCustomView(mediumTextView);
        mediumTab.setTabListener(listener);

        // Configure High priority tab
        ActionBar.Tab highTab = mActionBar.newTab();
        TextView highTextView = (TextView) getLayoutInflater().inflate(R.layout.tab_title, null);
        highTextView.setText("High");
        highTextView.setTypeface(mApplication.getTypeFace());
        highTab.setCustomView(highTextView);
        highTab.setTabListener(listener);

        mActionBar.addTab(allTab);
        mActionBar.addTab(mediumTab);
        mActionBar.addTab(highTab);

        mActionBar.selectTab(allTab);
    }

    private void loadList() {

        // Set the list based on which button was toggled
        QueryResult result;
        switch (mActionBar.getSelectedTab().getPosition()) {
            case 1:
                result = mApplication.getTodoItemsByPriority(1);
                break;
            case 2:
                result = mApplication.getTodoItemsByPriority(2);
                break;
            default:
                result = mApplication.getAllTodoItems();
        }

        mTodoItemList.clear();
        if (result != null) {
            for (DocumentRevision todoItem : result) {
                mTodoItemList.add(todoItem);
            }
        }
        mTodoItemAdapter.notifyDataSetChanged();
    }

    /**
     * Launches a dialog for adding a new TodoItem. Called when plus button is tapped.
     *
     * @param view The plus button that is tapped.
     */
    public void addTodo(View view) {
        // Create dialog pop-up
        final Dialog addDialog = new Dialog(this);

        // Set dialog configuration and show
        addDialog.setContentView(R.layout.add_edit_dialog);
        addDialog.setTitle("Add Todo");
        TextView textView = (TextView) addDialog.findViewById(android.R.id.title);
        if (textView != null) {
            textView.setGravity(Gravity.CENTER);
        }
        addDialog.setCancelable(true);
        Button add = (Button) addDialog.findViewById(R.id.Add);
        addDialog.show();

        // Set on click listener for done button to grab text entered by user and create a new list object
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText itemToAdd = (EditText) addDialog.findViewById(R.id.todo);
                final String name = itemToAdd.getText().toString();
                // If text was added, continue with normal operations
                if (!name.isEmpty()) {
                    mApplication.addTodoItem(name);

                    // Reload list for new data
                    loadList();
                }

                // Kill dialog when finished, or if no text was added
                addDialog.dismiss();
            }
        });
    }

    public void togglePush(MenuItem pushToggle) {

        if (push) {
            pushToggle.setTitle("Disable Push");
            //TODO Call disable push code
        } else {
            pushToggle.setTitle("Enable Push");
            //TODO Call enable push code
        }

        push = !push;
    }

    /**
     * Launches a dialog for updating the TodoItem name. Called when the list item is tapped.
     *
     * @param view The TodoItem that is tapped.
     */
    public void editTodoName(View view) {
        // Gets position in list view of tapped item
        final Integer pos = mListView.getPositionForView(view);

        // Same dialog creation as add, just change the title
        final Dialog addDialog = new Dialog(this);

        // Set dialog config and show
        addDialog.setContentView(R.layout.add_edit_dialog);
        addDialog.setTitle("Edit Todo");
        TextView textView = (TextView) addDialog.findViewById(android.R.id.title);
        if (textView != null) {
            textView.setGravity(Gravity.CENTER);
        }
        addDialog.setCancelable(true);
        EditText et = (EditText) addDialog.findViewById(R.id.todo);

        // Grab text to change from array adapter hash list
        final BasicDocumentRevision todoItem = (BasicDocumentRevision) mTodoItemList.get(pos);
        final String name = (String) todoItem.getBody().asMap().get(TODO_ITEM_NAME_KEY);
        et.setText(name);

        Button addDone = (Button) addDialog.findViewById(R.id.Add);
        addDialog.show();

        // Set on click listener for done button to save edited text and TodoItem
        addDone.setOnClickListener(new View.OnClickListener() {
            // Save text inputted when done is tapped
            @Override
            public void onClick(View view) {
                EditText editedText = (EditText) addDialog.findViewById(R.id.todo);

                // Grab newly edited name from the edit text box
                String newName = editedText.getText().toString();

                // If text is there, update the TodoItem DocumentRevision
                if (!newName.isEmpty()) {
                    mApplication.editTodoItem(todoItem, newName, null);

                    // Reload list for new data
                    loadList();
                }
                addDialog.dismiss();
            }
        });
    }

    /**
     * Increments the TodoItem priority and updates the list when the priority image for any item is tapped.
     *
     * @param view The TodoItem that has been tapped.
     */
    public void incrementTodoPriority(View view) {
        // Fetch position of item in list view
        Integer pos = mListView.getPositionForView(view);
        BasicDocumentRevision todoItem = (BasicDocumentRevision) mTodoItemList.get(pos);

        Map<String, Object> body = todoItem.getBody().asMap();
        int priority = (Integer) body.get(TODO_ITEM_PRIORITY_KEY);
        int newPriority = ++priority > 2 ? 0 : priority;

        mApplication.editTodoItem(todoItem, null, newPriority);
        loadList();
    }

    /**
     * Synchronize Datastore and remote database
     */
    private void sync() {
        final Replicator pullReplicator = mApplication.getPullReplicator();
        final Replicator pushReplicator = mApplication.getPushReplicator();

        // Start pull replication
        pullReplicator.getEventBus().register(new Object() {

            // After pull replication completes start push replication
            @Subscribe
            public void complete(ReplicationCompleted event) {
                pullReplicator.getEventBus().unregister(this);
                Log.d(TAG, String.format("Pull replication complete. %d documents replicated.", event.documentsReplicated));

                pushReplicator.getEventBus().register(new Object() {

                    // After push replication completes
                    @Subscribe
                    public void complete(ReplicationCompleted event) {
                        pushReplicator.getEventBus().unregister(this);
                        Log.d(TAG, String.format("Push replication complete. %d documents replicated.", event.documentsReplicated));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Reload list for new data
                                loadList();

                                // Notify refresh spinner that replication has stopped
                                mSwipeLayout.setRefreshing(false);
                            }
                        });
                    }

                    @Subscribe
                    public void error(ReplicationErrored event) {
                        pushReplicator.getEventBus().unregister(this);
                        Log.e(TAG, "Failed to complete push replication", event.errorInfo.getException());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                // Notify refresh spinner that replication has stopped
                                mSwipeLayout.setRefreshing(false);
                            }
                        });
                    }
                });
                pushReplicator.start();
            }

            @Subscribe
            public void error(ReplicationErrored event) {
                pullReplicator.getEventBus().unregister(this);
                Log.e(TAG, "Failed to complete pull replication", event.errorInfo.getException());

                // Notify refresh spinner that replication has stopped
                mSwipeLayout.setRefreshing(false);
            }
        });

        pullReplicator.start();
    }
}
