package com.ibm.mfp_bluelist_on_premises;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.cloudant.toolkit.Store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

/**
 * The {@code MainActivity} is the primary visual activity shown when the app is being interacted with. Most of the code is UI and visuals.
 */
public class MainActivity extends Activity {

    private static final String CLASS_NAME = MainActivity.class.getSimpleName();

    // Need filter value to know what and where to add items, basically maintains state of list view
    private Integer filter;
    // List of all TodoItem objects
    private ArrayList<TodoItem> allList;
    // Main List View
    private ListView lv;
    // Variable to hold swipe refresh layout
    private SwipeRefreshLayout swipeLayout;
    // List View Adapter for UI list data
    private SimpleAdapter simpleAdapter;
    // List of Hash pairs (DESCRIPTOR, VALUE), used for UI specific listing of image and text
    private List<HashMap<String,String>> adapterList;
    // Maintains list of medium priority TodoItems
    private ArrayList<TodoItem> mediumList;
    // Maintains list of high priority TodoItems
    private ArrayList<TodoItem> highList;

    // List of priority src images
    private int[] priority = new int[]{
            R.mipmap.low,
            R.mipmap.medium,
            R.mipmap.high,
    };

    // List of filter buttons
    private int[] filters = new int[]{
            R.id.button,
            R.id.button2,
            R.id.button3,
    };
    // Sparse Array to easily keep track and reference different priority lists
    private SparseArray<ArrayList<TodoItem>> filterLists;
    // Data Store Manager singleton to manipulate local store and get updated lists
    private DataStoreManager dataStoreManager;
    // Need to save activity to pass to DataStore Manager so Toasts can be called at appropriate times
    Activity main;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        // Create array to hold hash map
        adapterList = new ArrayList<>();
        // Set list view from xml to variable
        lv = (ListView) findViewById(R.id.listView);
        // List of strings to identify string keys in hashmap
        String[] from = {"priority","txt"};
        // List of xml views to set key values
        int[] to = {R.id.priority,R.id.txt};
        // Creating simple adapter to associate list of hashmaps with appropriate list view and items and allows for image and text to be created and interacted with in the listView
        simpleAdapter = new SimpleAdapter(getBaseContext(), adapterList, R.layout.listview_layout, from, to);
        // Set adapter to list View after configuration
        lv.setAdapter(simpleAdapter);
        // Set long click listener for list items to be deleted
        lv.setOnItemLongClickListener(deleteListener);
        // Create radio/toggle tabs above the list View for filtering
        ((RadioGroup) findViewById(R.id.toggleGroup)).setOnCheckedChangeListener(ToggleListener);
        // Grab Swipe Refresh Layout
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        // Set color scheme for swipe refresh listener
        swipeLayout.setColorSchemeResources(R.color.white, R.color.black, R.color.light_blue);
        // Set swipe refresh listener for sync on pull-down of the list
        swipeLayout.setOnRefreshListener(RefreshListener);
        // Could not get getResources().getColor to work, I believe there is a bug in that code, but this works
        swipeLayout.setProgressBackgroundColor(R.color.blue);

        main = this;

        if(savedInstanceState!=null && savedInstanceState.containsKey("filter")){
            // Set filter to currently selected tab
            filter = savedInstanceState.getInt("filter");
        }
        else {
            // Set default filter to "All"
            filter = 0;
        }

        // Grab DataStoreManager and update context and activity after splash screen
        dataStoreManager = DataStoreManager.getInstance(getApplicationContext(), main);
        dataStoreManager.setActivity(main);
        dataStoreManager.setContext(getApplicationContext());

        // Initialize priority lists
        mediumList = new ArrayList<>();
        highList = new ArrayList<>();

        // Initialize all list
        allList = new ArrayList<>();

        // Initialize filter lists
        filterLists = new SparseArray<>();
        filterLists.append(0, allList);
        filterLists.append(1, mediumList);
        filterLists.append(2, highList);

        // Called initially to populate UI lists
        restoreLocalData();
    }

    /**
     * RefreshListener to implement pull down sync functionality for list
     */
    private SwipeRefreshLayout.OnRefreshListener RefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            // Async task
            new DataBaseSync().execute();
        }
    };

    /**
     * Maintain state on activity destroyed, basically just maintaining the filter state
     * @param savedInstanceState the saved state
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        savedInstanceState.putInt("filter", filter);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * When Each Priority Button is toggled, sort the list appropriately
     * @param view The tapped toggle button.
     */
    public void onToggle(View view) {
        // Get parent of toggle button (Radio Group) to check the Id, without this the filter tabs do not maintain visually
        ((RadioGroup)view.getParent()).check(view.getId());
        // Grab text from the button to filter appropriately
        TextView tv = (TextView) view;
        String tab = tv.getText().toString();
        // Clear the list
        adapterList.clear();

        // If the "All" button is toggled, clear the list in the adapter and populate it with all the values from storage.
        if(tab.equals("All")){
            filter = 0;
            for(TodoItem todoItem : allList){
                HashMap<String,String> hm = new HashMap<>();
                hm.put("txt", todoItem.getName());
                hm.put("priority", Integer.toString(priority[todoItem.getPriority()]));
                adapterList.add(hm);
            }
        }

        // If the "Medium" button is toggled, clear the list in the adapter and populate it with all the values with priority 1 (medium) from storage.
        if(tab.equals("Medium")){
            filter = 1;
            for(TodoItem todoItem : mediumList){
                HashMap<String,String> hm = new HashMap<>();
                hm.put("txt", todoItem.getName());
                hm.put("priority", Integer.toString(priority[todoItem.getPriority()]));
                adapterList.add(hm);
            }
        }

        // If the "High" button is toggled, clear the list in the adapter and populate it with all the values with priority 2 (high) from storage.
        if(tab.equals("High")){
            filter = 2;
            for(TodoItem todoItem : highList){
                HashMap<String,String> hm = new HashMap<>();
                hm.put("txt", todoItem.getName());
                hm.put("priority", Integer.toString(priority[todoItem.getPriority()]));
                adapterList.add(hm);
            }
        }
        // Must inform the adapter whenever the data set has changed or else the appropriate data will not be displayed.
        simpleAdapter.notifyDataSetChanged();
    }


    /**
     * Radio Button Listener to ensure only one and always one sort button is toggled
     */
    static final RadioGroup.OnCheckedChangeListener ToggleListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final RadioGroup radioGroup, int i) {
            // Iterate through radio group
            for (int j = 0; j < radioGroup.getChildCount(); j++) {
                final ToggleButton view = (ToggleButton) radioGroup.getChildAt(j);
                // only allow one toggle to be selected
                view.setChecked(view.getId() == i);
                // disable the de-select toggle
                view.setEnabled(view.getId() != i);
            }
        }
    };

    /**
     * Restores and populates UI lists when a save, add, or delete fails to be consistent with local data store.
     */
    private void restoreLocalData(){
        allList.clear();
        allList.addAll(dataStoreManager.getTodoItemList());
        popLists();
    }

    /**
     * Long click listener for each item in the listView for delete functionality
     */
    private AdapterView.OnItemLongClickListener deleteListener = new AdapterView.OnItemLongClickListener(){
        @Override
        public boolean onItemLongClick(android.widget.AdapterView <?> parent, View view, int position, long id) {
            // Grab TodoItem to delete from current showing list
            TodoItem todoItemToDelete = filterLists.get(filter).get(position);
            // Remove from all List
            allList.remove(todoItemToDelete);
            // Remove from currently showing list
            filterLists.get(filter).remove(todoItemToDelete);
            // Remove the item in the actual visual list
            adapterList.remove(position);
            // Delete item from local store
            Store local = dataStoreManager.getTodosStore();
            local.delete(todoItemToDelete).continueWith(new Continuation<String, Void>() {

                @Override
                public Void then(Task<String> task) throws Exception {
                    // Log if the delete was cancelled.
                    if (task.isCancelled()){
                        Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");
                        // sync UI back with local store
                        restoreLocalData();
                    }
                    // Log error message, if the delete task fails.
                    else if (task.isFaulted()) {
                        Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                        // sync UI back with local store
                        restoreLocalData();
                    }
                    // If the result succeeds, reload the list.
                    else {
                        popLists();
                        Log.i("STORE_CHANGE:","Item deleted from local store " + task.getResult());
                    }
                    return null;
                }
            },Task.UI_THREAD_EXECUTOR);
            return true;
        }
    };


    /**
     * Add function called when the plus in the upper left hand corner is pressed.
     * The button creates a new pop-up dialog for the user to enter their new TodoItem.
     * When the 'Done' button is tapped, a new TodoItem is created in the UI and in the local data store.
     * @param view The list item that is tapped.
     */
    public void addTodo(View view){
        // Create dialog pop-up
        final Dialog addDialog = new Dialog(this);

        // Set dialog configuration and show
        addDialog.setContentView(R.layout.add_edit_dialog);
        addDialog.setTitle("Add Todo");
        TextView textView = (TextView) addDialog.findViewById(android.R.id.title);
        if(textView != null) {
            textView.setGravity(Gravity.CENTER);
        }
        addDialog.setCancelable(true);
        Button add =(Button) addDialog.findViewById(R.id.Add);
        addDialog.show();

        // Set on click listener for done button to grab text entered by user and create a new list object
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText itemToAdd =(EditText) addDialog.findViewById(R.id.todo);
                String toAdd = itemToAdd.getText().toString();
                // If text was added, continue with normal operations
                if(!toAdd.isEmpty()) {
                    // Create the new TodoItem
                    TodoItem todoToAdd = new TodoItem(toAdd, filter);
                    // Save new TodoItem to local store
                    Store store = dataStoreManager.getTodosStore();
                    store.save(todoToAdd).continueWith(new Continuation<Object, Void>() {

                        @Override
                        public Void then(Task<Object> task) throws Exception {
                            // Log if the save was cancelled.
                            if (task.isCancelled()) {
                                Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");
                                // sync UI back with local store
                                restoreLocalData();
                            }
                            // Log error message, if the save task fails.
                            else if (task.isFaulted()) {
                                Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                                // sync UI back with local store
                                restoreLocalData();
                            }

                            // If the result succeeds, add the new TodoItem and load the list.
                            else {
                                allList.add((TodoItem) task.getResult());
                                popLists();
                                Log.i("STORE_CHANGE:","Item added to local store " + task.getResult());
                            }
                            return null;
                        }

                    });
                }
                // Kill dialog when finished, or if no text was added
                addDialog.dismiss();
            }
        });
    }

    /**
     * Edit function called when a list item is tapped. Very similar to add in implementation.
     * @param view The list item that is tapped.
     */
    public void editTodo(View view){
        // Gets position in list view of tapped item
        final Integer pos = lv.getPositionForView(view);
        // Same dialog creation as add, just change the title
        final Dialog addDialog = new Dialog(this);
        // Set dialog config and show
        addDialog.setContentView(R.layout.add_edit_dialog);
        addDialog.setTitle("Edit Todo");
        TextView textView = (TextView) addDialog.findViewById(android.R.id.title);
        if(textView != null)
        {
            textView.setGravity(Gravity.CENTER);
        }
        addDialog.setCancelable(true);
        EditText et =(EditText) addDialog.findViewById(R.id.todo);
        // Grab text to change from array adapter hash list
        et.setText(adapterList.get(pos).get("txt"));
        Button addDone =(Button) addDialog.findViewById(R.id.Add);
        addDialog.show();
        // Set on click listener for done button to save edited text and TodoItem
        addDone.setOnClickListener(new View.OnClickListener() {
            // Save text inputted when done is tapped
            @Override
            public void onClick(View view) {
                EditText editedText = (EditText) addDialog.findViewById(R.id.todo);
                // Grab newly edited text from the edit text box
                String newText = editedText.getText().toString();
                // If text is there, continue with normal operations
                if (!newText.isEmpty()) {
                    // remove the TodoItem from main list and update the name
                    ArrayList<TodoItem> listShowing = filterLists.get(filter);
                    TodoItem toEdit = listShowing.get(pos);
                    allList.remove(toEdit);
                    toEdit.setName(newText);
                    // Save edited TodoItem in local store
                    Store store = dataStoreManager.getTodosStore();
                    store.save(toEdit).continueWith(new Continuation<Object, Void>() {

                        @Override
                        public Void then(Task<Object> task) throws Exception {
                            // Log if the save was cancelled.
                            if (task.isCancelled()) {
                                Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");
                                // sync UI back with local store
                                restoreLocalData();
                            }
                            // Log error message, if the save task fails.
                            else if (task.isFaulted()) {
                                Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                                // sync UI back with local store
                                restoreLocalData();
                            }

                            // If the result succeeds, add the newly updated TodoItem and load the list.
                            else {
                                // WARNING: Do not use the same object you passed into the store after you save the update. Always use the new object from task.getResult() to avoid errors and inconsistencies
                                allList.add((TodoItem) task.getResult());
                                popLists();
                                Log.i("STORE_CHANGE:","Item edited in local store " + task.getResult());
                            }
                            return null;
                        }

                       });
                }
                addDialog.dismiss();
            }
        });
    }

    /**
     * Populates array lists for easier sorting and notifies the activity of a filter change to update the list
     */
    private void popLists(){

        if(allList!=null){
            dataStoreManager.sortItems(allList);
            mediumList.clear();
            highList.clear();
            for(TodoItem todoItem :allList){
                if(todoItem.getPriority()==1){
                    mediumList.add(todoItem);
                }
                if(todoItem.getPriority()>1){
                    highList.add(todoItem);
                }
            }

            filterLists.clear();

            filterLists.append(0, allList);
            filterLists.append(1, mediumList);
            filterLists.append(2, highList);

            // Update visual list using onToggle when filter has yet to be tapped
            onToggle(findViewById(filters[filter]));

        }



    }

    /**
     * Priority change function called when the image for any item is tapped.
     * Changes both the color of the circle and the priority associated in the TodoItem object in the UI and local data store.
     * @param view The TodoItem that has been tapped.
     */
    public void priorityChange(View view){
        // Fetch position of item in list view
        Integer pos = lv.getPositionForView(view);
        ArrayList<TodoItem> listShowing = filterLists.get(filter);
        TodoItem toUpdate = listShowing.get(pos);
        // remove from all list
        allList.remove(toUpdate);
        // Update priority
        toUpdate.priorityCycle();

        // Save edited TodoItem in local store
        Store store = dataStoreManager.getTodosStore();
        store.save(toUpdate).continueWith(new Continuation<Object, Void>() {

            @Override
            public Void then(Task<Object> task) throws Exception {
                // Log if the save was cancelled.
                if (task.isCancelled()) {
                    Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");
                    // sync UI back with local store
                    restoreLocalData();
                }
                // Log error message, if the save task fails.
                else if (task.isFaulted()) {
                    Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                    // sync UI back with local store
                    restoreLocalData();
                }
                // If the result succeeds, load the list.
                else {
                   allList.add((TodoItem) task.getResult());
                    Log.i("STORE_CHANGE:","Item edited in local store " + task.getResult());
                    popLists();
                }
                return null;
            }

        });
    }

    /**
     * Async task to run pull and push replication in a background thread
     */
    private class DataBaseSync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids){
            dataStoreManager.sync(true);
            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
            restoreLocalData();
            // Need to notify refresh spinner that replication has completed
            swipeLayout.setRefreshing(false);
        }
    }

}
