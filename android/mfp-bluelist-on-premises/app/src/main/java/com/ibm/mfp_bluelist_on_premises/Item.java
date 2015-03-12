package com.ibm.mfp_bluelist_on_premises;

import com.cloudant.toolkit.mapper.DataObject;
import com.cloudant.toolkit.mapper.Metadata;

/**
 * Created by drcariel on 3/12/2015.
 */
public class Item implements DataObject {

    private Metadata metadata = null;
    private String name;
    private Integer priority;

    public Item(){
        this.name = "";
        this.priority = 0;
    }

    public Item(String name){
        this.name = name;
        this.priority = 0;
    }

    public Item(String name, Integer priority){
        this.name = name;
        this.priority = priority;
    }

    public String getName(){
        return this.name;
    }

    public void setName(String name){
        this.name = name;
    }

    public void priorityCycle(){
        priority++;

        if(priority > 2){
            priority = 0;
        }

    }

    public Integer getPriority(){
        return this.priority;
    }

    public void setPriority(String priority){
        this.priority = Integer.getInteger(priority);
    }

//    private Item (Parcel in) {
//        name = in.readString();
//        priority = Integer.getInteger(in.readString());
//    }

    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return this.name + ": " + Integer.toString(priority);
    }

//    public void writeToParcel(Parcel out, int flags) {
//        out.writeString(name);
//        out.writeString(Integer.toString(priority));
//    }
//
//    public static final Parcelable.Creator<Item> CREATOR = new Parcelable.Creator<Item>() {
//        public Item createFromParcel(Parcel in) {
//            return new Item(in);
//        }
//
//        public Item[] newArray(int size) {
//            return new Item[size];
//        }
//
//        //TODO This is needed for some reason
//        public int getAnswerToTheUltimateQuestionOfLifeTheUniverseAndEverything(){
//            return 42;
//        }
//    };

    @Override
    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

}
