package com.ibm.mfp_bluelist_on_premises;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.cloudant.toolkit.Store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by drcariel on 3/12/2015.
 *
 * TODO: Exception is thrown when the priority is tapped 4+ times quickly in a row, store gets out of sync but is able to recover
 * TODO: The actual local store save/delete/edit boltz tasks can be moved to DataStoreManager
 * TODO: Am I able to handle rotation at the moment? Does the DataStoreManager singleton stick around?
 * TODO: Update filter functionality to be more centralized and concise
 */
public class MainActivity extends Activity {

    private static final String CLASS_NAME = MainActivity.class.getSimpleName();

    // Splash Dialog
    protected Dialog bluemixSplash;
    // Need filter value to know what and where to add items, basically maintains state of list view
    private String filter;
    // List of all todo objects
    private ArrayList<Item> allList;
    // Main List View
    private ListView lv;

    private SwipeRefreshLayout swipeLayout;
    // List View Adapter for UI list data
    private SimpleAdapter simpleAdapter;
    // List of Hash pairs (DESCRIPTOR, VALUE), used for UI specific listing of image and text
    private List<HashMap<String,String>> adapterList;
    // Maintains list of medium priority todos
    private ArrayList<Item> mediumList;
    // Maintains list of high priority todos
    private ArrayList<Item> highList;

    // List of priority src images
    private int[] priority = new int[]{
            R.mipmap.low,
            R.mipmap.medium,
            R.mipmap.high,
    };

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

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);

        swipeLayout.setOnRefreshListener(RefreshListener);

        if(savedInstanceState!=null && savedInstanceState.containsKey("allData") && savedInstanceState.containsKey("mediumData") && savedInstanceState.containsKey("highData")){

//            allList = savedInstanceState.getParcelableArrayList("allData");
//            mediumList = savedInstanceState.getParcelableArrayList("mediumData");
//            highList = savedInstanceState.getParcelableArrayList("highData");

            // Set filter to currently selected tab
            filter = savedInstanceState.getCharSequence("filter").toString();

            // TODO: Need to create filter method to populate list visually
            if(filter.equals("All")){
                onToggle(findViewById(R.id.button));
            }

            if(filter.equals("Medium")){
                onToggle(findViewById(R.id.button2));
            }

            if(filter.equals("High")){
                onToggle(findViewById(R.id.button3));
            }
        }
        else {

            // Initialize priority lists
            mediumList = new ArrayList<Item>();
            highList = new ArrayList<Item>();

            // Initialize all list
            allList = new ArrayList<Item>();

            // Set default filter to "All"
            filter = "All";

            allList = DataStoreManager.getInstance(getApplicationContext()).getItemList();

            popLists();

            onToggle(findViewById(R.id.button));


        }

    }

    private SwipeRefreshLayout.OnRefreshListener RefreshListener = new SwipeRefreshLayout.OnRefreshListener() {

        @Override
        public void onRefresh() {
            // TODO: Could I make this into a separate thred? another async task? Currently, the spinner freezes for a moment waiting for sync
            DataStoreManager.getInstance(getApplicationContext()).sync(true);

            DataStoreManager.getInstance(getApplicationContext()).setItemList();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {

                    swipeLayout.setRefreshing(false);
                }
            }, 3000);
            allList.clear();
            allList = DataStoreManager.getInstance(getApplicationContext()).getItemList();
            popLists();
        }
    };

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){

//        savedInstanceState.putParcelableArrayList("allData", allList);
//        savedInstanceState.putParcelableArrayList("mediumData", mediumList);
//        savedInstanceState.putParcelableArrayList("highData", highList);

        savedInstanceState.putCharSequence("filter", filter);

        super.onSaveInstanceState(savedInstanceState);

    }

    /**
     * When Each Priority Button is toggled, sort the list appropriately
     * @param view
     */
    public void onToggle(View view) {
        // Get parent of toggle button (Radio Group) to check the Id, without this the filter tabs do not maintain visually
        ((RadioGroup)view.getParent()).check(view.getId());
        // Grab text from the button to filter appropriately
        TextView tv = (TextView) view;
        filter = tv.getText().toString();
        // Clear the list
        adapterList.clear();
        // If the "All" button is toggled, clear the list in the adapter and populate it with all the values from storage.
        if(filter.equals("All")){

            for(Item item: allList){
                HashMap hm = new HashMap<String, String>();
                hm.put("txt", item.getName());
                hm.put("priority", Integer.toString(priority[item.getPriority()]));
                adapterList.add(hm);
            }

        }
        // If the "Medium" button is toggled, clear the list in the adapter and populate it with all the values with priority 1 (medium) from storage.
        if(filter.equals("Medium")){

            for(Item item: mediumList){
                HashMap hm = new HashMap<String, String>();
                hm.put("txt", item.getName());
                hm.put("priority", Integer.toString(priority[item.getPriority()]));
                adapterList.add(hm);
            }

        }
        // If the "High" button is toggled, clear the list in the adapter and populate it with all the values with priority 2 (high) from storage.
        if(filter.equals("High")){

            for(Item item: highList){
                HashMap hm = new HashMap<String, String>();
                hm.put("txt", item.getName());
                hm.put("priority", Integer.toString(priority[item.getPriority()]));
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

    // Long click listener for each item in the listView for delete functionality
    private AdapterView.OnItemLongClickListener deleteListener = new AdapterView.OnItemLongClickListener(){
        @Override
        public boolean onItemLongClick(android.widget.AdapterView <?> parent, View view, int position, long id) {
            // If all Todo items are showing, remove from appropriate lists
            Item t = null;
            if(filter.equals("All")){
                t = allList.get(position);
            }
            // If medium priority todos are showing, remove from appropriate lists
            else if(filter.equals("Medium")){
                t = mediumList.get(position);
            }
            // If high todos are showing, remove from appropriate lists
            else{
                t = highList.get(position);
            }

            if(t.getPriority() == 1){
                mediumList.remove(t);
            }
            if(t.getPriority() == 2){
                highList.remove(t);
            }

            allList.remove(t);

            // Remove the item in the list
            adapterList.remove(position);

            Store todosStore = DataStoreManager.getInstance(getApplicationContext()).getStore();

            todosStore.delete(t).continueWith(new Continuation<String, Void>() {

                @Override
                public Void then(Task<String> task) throws Exception {
                    // Log if the delete was cancelled.
                    if (task.isCancelled()){
                        Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");
                        Toast.makeText(MainActivity.this, "Delete From Local Store was Cancelled", Toast.LENGTH_LONG).show();

                    }

                    // Log error message, if the delete task fails.
                    else if (task.isFaulted()) {
                        Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                        Toast.makeText(MainActivity.this, "Error Deleting From Local Store: "+ task.getError().getMessage(), Toast.LENGTH_LONG).show();

                    }

                    // If the result succeeds, reload the list.
                    else {
                        Toast.makeText(MainActivity.this, "Todo Deleted From Local Store", Toast.LENGTH_SHORT).show();

                        DataStoreManager.getInstance(getApplicationContext()).setItemList();
                        //allList.clear();
                        allList = DataStoreManager.getInstance(getApplicationContext()).getItemList();

                    }
                    return null;
                }
            },Task.UI_THREAD_EXECUTOR);

            popLists();

            // Must inform the adapter whenever the data set has changed or else the appropriate data will not be displayed.
            simpleAdapter.notifyDataSetChanged();

            return true;
        }
    };


    /**
     * Add function called when the plus in the upper left hand corner is pressed.
     * The button creates a new pop-up dialog for the user to enter their new todo.
     * @param view
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
                    Item todoToAdd = null;
                    // If "All" tab is selected set the priority to low
                    if(filter.equals("All")) {
                        hm.put("priority", Integer.toString(priority[0]));

                        todoToAdd = new Item(toAdd);
                    }
                    // If "Medium" tab is selected set the priority to medium
                    if(filter.equals("Medium")) {
                        hm.put("priority", Integer.toString(priority[1]));

                        todoToAdd = new Item(toAdd, 1);

                        // Add new Todo to medium priority list
                        mediumList.add(todoToAdd);
                    }
                    // If "High" tab is selected set the priority to medium
                    if(filter.equals("High")) {
                        hm.put("priority", Integer.toString(priority[2]));

                        todoToAdd = new Item(toAdd, 2);
                        // Add new Todo to high priority list
                        highList.add(todoToAdd);
                    }
                    // Add new Todo item to the adapter
                    allList.add(todoToAdd);
                    adapterList.add(hm);

                    Store todosStore = DataStoreManager.getInstance(getApplicationContext()).getStore();

                    todosStore.save(todoToAdd).continueWith(new Continuation<Object, Void>() {

                        @Override
                        public Void then(Task<Object> task) throws Exception {
                            // Log if the save was cancelled.
                            if (task.isCancelled()){
                                Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");
                                Toast.makeText(MainActivity.this, "Todo Creation Was Cancelled", Toast.LENGTH_LONG).show();

                            }
                            // Log error message, if the save task fails.
                            else if (task.isFaulted()) {

                                Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                                Toast.makeText(MainActivity.this, "Error Saving New Todo in Local Store: " + task.getError().getMessage(), Toast.LENGTH_LONG).show();

                            }

                            // If the result succeeds, load the list.
                            else {

                                Toast.makeText(MainActivity.this, "Todo Created in Local Store", Toast.LENGTH_SHORT).show();
                                //allList.clear();
                                DataStoreManager.getInstance(getApplicationContext()).setItemList();
                                allList = DataStoreManager.getInstance(getApplicationContext()).getItemList();

                            }
                            return null;
                        }

                    });

                    popLists();

                    // Make sure adapter knows display data has been changed
                    simpleAdapter.notifyDataSetChanged();

                    Log.d("Successfully added-->", toAdd);
                }
                // Kill dialog when finished, or if no text was added
                addDialog.dismiss();
            }
        });
    }

    /**
     * Edit function called when a list item is tapped. Very similar to add in implementation.
     * @param view
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

                    allList.get(pos).setName(newText);

                    adapterList.get(pos).put("txt", newText);

                    Store todosStore = DataStoreManager.getInstance(getApplicationContext()).getStore();

                    todosStore.save(allList.get(pos)).continueWith(new Continuation<Object, Void>() {

                        @Override
                        public Void then(Task<Object> task) throws Exception {
                            if(task.isCancelled()) {
                                Log.e(CLASS_NAME, "Exception : " + task.toString() + " was cancelled.");
                                Toast.makeText(MainActivity.this, "Edit Was Cancelled", Toast.LENGTH_LONG).show();

                            }

                            else if (task.isFaulted()) {
                                Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                                Toast.makeText(MainActivity.this, "Error Editing Todo in Local Store" + task.getError().getMessage(), Toast.LENGTH_LONG).show();

                            }

                            else {
                                Toast.makeText(MainActivity.this, "Todo Edited Successfully", Toast.LENGTH_SHORT).show();

                                //allList.clear();
                                DataStoreManager.getInstance(getApplicationContext()).setItemList();
                                allList = DataStoreManager.getInstance(getApplicationContext()).getItemList();

                            }
                            return null;
                        }

                    },Task.UI_THREAD_EXECUTOR);

                    // If I dont call this I get an exception when I attempt to edit the same object more than once before syncing

                    popLists();
                    // Make sure adapter knows display data has been changed
                    simpleAdapter.notifyDataSetChanged();
                    Log.d("Successfully changed-->", newText);
                }
                addDialog.dismiss();
            }
        });
    }

    private void popLists(){
        if(allList!=null){
            mediumList.clear();
            highList.clear();
            for(Item item:allList){
                if(item.getPriority()==1){
                    mediumList.add(item);
                }
                if(item.getPriority()>1){
                    highList.add(item);
                }
            }
        }
    }

    /**
     * Priority change function called when the image for any item is tapped.
     * Changes both the color of the circle and the priority associated in the todo object
     * @param view
     */
    public void priorityChange(View view){
        // Fetch position of item in list view?
        Integer pos = lv.getPositionForView(view);
        // If the "All" tab is selected
        Item t = null;

        if(filter.equals("All")) {
            // Grab list item Todo object
            t = allList.get(pos);
            // Move Todo object to appropriate list based on current priority
            if(t.getPriority() == 1){
                highList.add(t);
                mediumList.remove(t);
            }
            if(t.getPriority() == 2){
                highList.remove(t);
            }
            if(t.getPriority()==0){
                mediumList.add(t);
            }
            // Change the priority on the Todo object itself
            t.priorityCycle();
            // Change the priority color to be in sync with the associated Todo object
            adapterList.get(pos).put("priority", Integer.toString(priority[t.getPriority()]));
        }
        // If the "Medium" tab is selected move the object to high priority list and cycle the priority to high
        if(filter.equals("Medium")) {
            t = mediumList.get(pos);
            t.priorityCycle();
            adapterList.remove(adapterList.get(pos));
            highList.add(t);
            mediumList.remove(t);
        }
        // If the "High" tab is selected remove the object from the high priority list and cycle the priority to low
        if(filter.equals("High")) {
            t = highList.get(pos);
            t.priorityCycle();
            adapterList.remove(adapterList.get(pos));
            highList.remove(t);
        }

        Store todosStore = DataStoreManager.getInstance(getApplicationContext()).getStore();

        todosStore.save(t).continueWith(new Continuation<Object, Void>() {

            @Override
            public Void then(Task<Object> task) throws Exception {
                if(task.isCancelled()) {
                    Log.e(CLASS_NAME, "Exception : " + task.toString() + " was cancelled.");
                    Toast.makeText(MainActivity.this, "Edit Was Cancelled", Toast.LENGTH_LONG).show();

                }

                else if (task.isFaulted()) {
                    Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                    Toast.makeText(MainActivity.this, "Error Editing Todo in Local Store" + task.getError().getMessage(), Toast.LENGTH_LONG).show();

                }

                else {
                    Toast.makeText(MainActivity.this, "Todo Edited Successfully", Toast.LENGTH_SHORT).show();
                    //allList.clear();
                    DataStoreManager.getInstance(getApplicationContext()).setItemList();
                    allList = DataStoreManager.getInstance(getApplicationContext()).getItemList();

                }
                return null;
            }

        },Task.UI_THREAD_EXECUTOR);

        popLists();

        // Make sure adapter knows display data has been changed
        simpleAdapter.notifyDataSetChanged();
    }
    public String getFilter(){
        return filter;
    }

}
