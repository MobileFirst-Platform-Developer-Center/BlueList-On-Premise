package com.ibm.mfp_bluelist_on_premises;

import com.cloudant.toolkit.mapper.DataObject;
import com.cloudant.toolkit.mapper.Metadata;

/**
 * Basic TodoItem Object that extends DataObject.
 * Must have metadata when implementing DataObject
 */
public class TodoItem implements DataObject {

    private Metadata metadata = null;
    private String name;
    private Integer priority;

    /**
     * Basic empty constructor is required by the object mapper if another constructor is used
     */
    public TodoItem(){
        this.name = "";
        this.priority = 0;
    }

    /**
     * Constructor that sets the name and default priority
     * @param name String name of the TodoItem
     */
    public TodoItem(String name){
        this.name = name;
        this.priority = 0;
    }

    /**
     * Constructor that sets the name and priority
     * @param name String name of the TodoItem
     * @param priority Integer priority for TodoItem
     */
    public TodoItem(String name, Integer priority){
        this.name = name;
        this.priority = priority;
    }

    /**
     * Grabs the TodoItem name
     * @return String name
     */
    public String getName(){
        return this.name;
    }

    /**
     * Sets the name of the TodoItem
     * @param name String of name to change to
     */
    public void setName(String name){
        this.name = name;
    }

    /**
     * Cycles through the priorities 0, 1, and 2 for low, medium, and high respectively
     */
    public void priorityCycle(){
        priority++;
        if(priority > 2){
            priority = 0;
        }
    }

    /**
     * Grabs current priority for the TodoItem
     * @return Integer priority
     */
    public Integer getPriority(){
        return this.priority;
    }

    /**
     * Grab the metadata stored in the TodoItem
     * @return Metadata object
     */
    @Override
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the Metadata object that stores information on the revision and verison of the object for the mapper
     * @param metadata Metadata to be set in the TodoObject
     */
    @Override
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

}
