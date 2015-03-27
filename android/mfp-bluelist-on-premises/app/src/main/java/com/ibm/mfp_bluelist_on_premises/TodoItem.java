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
