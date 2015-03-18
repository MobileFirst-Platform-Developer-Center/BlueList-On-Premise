package com.ibm.mfp_bluelist_on_premises;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
    // List of all todo objects
    private ArrayList<TodoItem> allList;
    // Main List View
    private ListView lv;
    // Variable to hold swipe refresh layout
    private SwipeRefreshLayout swipeLayout;
    // List View Adapter for UI list data
    private SimpleAdapter simpleAdapter;
    // List of Hash pairs (DESCRIPTOR, VALUE), used for UI specific listing of image and text
    private List<HashMap<String,String>> adapterList;
    // Maintains list of medium priority todos
    private ArrayList<TodoItem> mediumList;
    // Maintains list of high priority todos
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

    private SparseArray<ArrayList<TodoItem>> filterLists;
    // Data Store Manager singleton to manipulate local store and get updated lists
    private DataStoreManager dsm;
    // Need to save activity to pass to DataStore Manager so Toasts can be called at appropriate times
    Activity main;

    /**
     * Instantiate UI when activity starts
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Create array to hold hash map
        adapterList = new ArrayList<HashMap<String,String>>();
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
        // Grab Swiperefresh Layout
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        // Set swipe refresh listener for sync on pulldown of the list
        swipeLayout.setOnRefreshListener(RefreshListener);

        main = this;

        if(savedInstanceState!=null && savedInstanceState.containsKey("allData") && savedInstanceState.containsKey("mediumData") && savedInstanceState.containsKey("highData")){

            //TODO: Am I able to handle rotation at the moment? Does the DataStoreManager singleton stick around?
//            allList = savedInstanceState.getParcelableArrayList("allData");
//            mediumList = savedInstanceState.getParcelableArrayList("mediumData");
//            highList = savedInstanceState.getParcelableArrayList("highData");

            // Set filter to currently selected tab
            filter = savedInstanceState.getInt("filter");

            popLists();
        }
        else {

            dsm = DataStoreManager.getInstance(getApplicationContext(), main);

            dsm.setActivity(main);

            dsm.setContext(getApplicationContext());

            // Initialize priority lists
            mediumList = new ArrayList<TodoItem>();
            highList = new ArrayList<TodoItem>();

            // Initialize all list
            allList = new ArrayList<TodoItem>();

            filterLists = new SparseArray<ArrayList<TodoItem>>();

            filterLists.append(0, allList);
            filterLists.append(1, mediumList);
            filterLists.append(2, highList);

            // Set default filter to "All"
            filter = 0;

            allList.addAll(dsm.getTodoItemList());

            popLists();

        }

    }

    /**
     * Update visual list using onToggle when filter has yet to be tapped
     */
    private void filterChange(){
        onToggle(findViewById(filters[filter]));
    }

    /**
     * RefreshListener to implement pull down sync functionality for list
     */
    private SwipeRefreshLayout.OnRefreshListener RefreshListener = new SwipeRefreshLayout.OnRefreshListener() {

        @Override
        public void onRefresh() {
            new DataBaseSync().execute();

            //TODO: Is this done correctly? Can I do this more accurately?
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {

                    swipeLayout.setRefreshing(false);
                }
            }, 4500);
        }
    };

    /**
     *
     * @param savedInstanceState
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){

//        savedInstanceState.putParcelableArrayList("allData", allList);
//        savedInstanceState.putParcelableArrayList("mediumData", mediumList);
//        savedInstanceState.putParcelableArrayList("highData", highList);

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
                HashMap<String,String> hm = new HashMap<String, String>();
                hm.put("txt", todoItem.getName());
                hm.put("priority", Integer.toString(priority[todoItem.getPriority()]));
                adapterList.add(hm);
            }

        }
        // If the "Medium" button is toggled, clear the list in the adapter and populate it with all the values with priority 1 (medium) from storage.
        if(tab.equals("Medium")){

            filter = 1;

            for(TodoItem todoItem : mediumList){
                HashMap<String,String> hm = new HashMap<String, String>();
                hm.put("txt", todoItem.getName());
                hm.put("priority", Integer.toString(priority[todoItem.getPriority()]));
                adapterList.add(hm);
            }

        }
        // If the "High" button is toggled, clear the list in the adapter and populate it with all the values with priority 2 (high) from storage.
        if(tab.equals("High")){

            filter = 2;

            for(TodoItem todoItem : highList){
                HashMap<String,String> hm = new HashMap<String, String>();
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
     * Long click listener for each item in the listView for delete functionality
     */
    private AdapterView.OnItemLongClickListener deleteListener = new AdapterView.OnItemLongClickListener(){
        @Override
        public boolean onItemLongClick(android.widget.AdapterView <?> parent, View view, int position, long id) {

            TodoItem todoItemToDelete = filterLists.get(filter).get(position);

            allList.remove(todoItemToDelete);

            filterLists.get(filter).remove(todoItemToDelete);
            // Remove the item in the list
            adapterList.remove(position);
            // Delete item from local store
            Store local = dsm.getTodosStore();

            local.delete(todoItemToDelete).continueWith(new Continuation<String, Void>() {

                @Override
                public Void then(Task<String> task) throws Exception {
                    // Log if the delete was cancelled.
                    if (task.isCancelled()){
                        Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");

                    }
                    // Log error message, if the delete task fails.
                    else if (task.isFaulted()) {
                        Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());

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
        //Create dialog pop-up
        final Dialog addDialog = new Dialog(this);
        // Set dialog to appropriate xml layout
        addDialog.setContentView(R.layout.add_edit_dialog);
        addDialog.setTitle("Add Todo");
        TextView textView = (TextView) addDialog.findViewById(android.R.id.title);
        if(textView != null)
        {
            textView.setGravity(Gravity.CENTER);
        }
        //Allow user to cancel by tapping outside of dialog or pressing back button (Did not implement a cancel button)
        addDialog.setCancelable(true);
        // Create done button in dialog
        Button add =(Button) addDialog.findViewById(R.id.Add);
        // Show dialog
        addDialog.show();
        // Set on click listener for done button to grab text entered by user and create a new list object
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Grab text box
                EditText itemToAdd =(EditText) addDialog.findViewById(R.id.todo);
                // Grab user inputted text
                String toAdd = itemToAdd.getText().toString();
                // If text was added, continue with normal operations
                if(!toAdd.isEmpty()) {
                    // Create new HashMap
                    HashMap<String, String> hm = new HashMap<String, String>();
                    // Set text to user inputted text
                    hm.put("txt", toAdd);
                    // Create variable to store newly created Todo
                    TodoItem todoToAdd = null;

                    hm.put("priority", Integer.toString(priority[filter]));

                    todoToAdd = new TodoItem(toAdd, filter);

                    Store store = dsm.getTodosStore();

                    store.save(todoToAdd).continueWith(new Continuation<Object, Void>() {

                        @Override
                        public Void then(Task<Object> task) throws Exception {
                            // Log if the save was cancelled.
                            if (task.isCancelled()) {
                                Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");

                            }
                            // Log error message, if the save task fails.
                            else if (task.isFaulted()) {

                                Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());

                            }

                            // If the result succeeds, load the list.
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
        // Set dialog to appropriate xml layout
        addDialog.setContentView(R.layout.add_edit_dialog);
        addDialog.setTitle("Edit Todo");
        TextView textView = (TextView) addDialog.findViewById(android.R.id.title);
        if(textView != null)
        {
            textView.setGravity(Gravity.CENTER);
        }
        addDialog.setCancelable(true);
        // Need to populate text box with current todo text that the user wants to edit
        EditText et =(EditText) addDialog.findViewById(R.id.todo);
        // Grab text to change from array adapter hash list
        et.setText(adapterList.get(pos).get("txt"));
        // Create "Done" button for pop-up dialog
        Button addDone =(Button) addDialog.findViewById(R.id.Add);
        // Show dialog
        addDialog.show();
        // Set on click listener for dialog
        addDone.setOnClickListener(new View.OnClickListener() {
            // Save text inputted when done is tapped
            @Override
            public void onClick(View view) {
                // Grab the edit text box
                EditText editedText = (EditText) addDialog.findViewById(R.id.todo);
                // Grab newly edited text from the edit text box
                String newText = editedText.getText().toString();
                // Make sure there is text in the Edit Text box
                if (!newText.isEmpty()) {

                    ArrayList<TodoItem> listShowing = filterLists.get(filter);

                    TodoItem toEdit = listShowing.get(pos);

                    allList.remove(toEdit);

                    toEdit.setName(newText);

                    Store store = dsm.getTodosStore();

                    store.save(toEdit).continueWith(new Continuation<Object, Void>() {

                        @Override
                        public Void then(Task<Object> task) throws Exception {
                            // Log if the save was cancelled.
                            if (task.isCancelled()) {
                                Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");

                            }
                            // Log error message, if the save task fails.
                            else if (task.isFaulted()) {

                                Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());

                            }

                            // If the result succeeds, load the list.
                            else {
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
     * Populates arraylists for easier sorting and notifies the activity of a filter change to update the list
     */
    private void popLists(){

        if(allList!=null){
            dsm.sortItems(allList);
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

            filterChange();

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

        allList.remove(toUpdate);

        toUpdate.priorityCycle();

        Store store = dsm.getTodosStore();

        store.save(toUpdate).continueWith(new Continuation<Object, Void>() {

            @Override
            public Void then(Task<Object> task) throws Exception {
                // Log if the save was cancelled.
                if (task.isCancelled()) {
                    Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");

                }
                // Log error message, if the save task fails.
                else if (task.isFaulted()) {

                    Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());

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

            dsm.sync(true);
            return null;
        }

        @Override
        protected void onPostExecute(Void params) {

            popLists();
        }
    }

}
