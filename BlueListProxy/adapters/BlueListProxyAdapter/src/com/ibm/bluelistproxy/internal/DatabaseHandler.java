/*
 * IBM Confidential OCO Source Materials
 *
 * 5725-I43 Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *
*/
package com.ibm.bluelistproxy.internal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Hex;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.IndexField;
import com.cloudant.client.api.model.IndexField.SortOrder;

/**
 * Processes all database related requests.
 * This includes create database (along with views and index) and delete database.
 */
public class DatabaseHandler {
	private static final String CLASS_NAME = DatabaseHandler.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);
    
    /**
     * Build a bluelist sample database name for the given user.
     * The first part of the name is 'todosdb'.
     * The second part of the name is generated based on the user name using SHA1.
     * 
     * @param userName The user name
     * @return The generated database name
     * @throws BlueListProxyException Thrown if there is a SHA1 failure
     */
    public static String getDatabaseName(String userName)
    throws BlueListProxyException {
		final String METHOD_NAME = "getDatabaseName";
		logger.entering(CLASS_NAME, METHOD_NAME, userName);
		
		String databaseName = "todosdb_";
		try{
			MessageDigest md = MessageDigest.getInstance("SHA1");
			md.update(userName.getBytes());
			databaseName += Hex.encodeHexString(md.digest());
			logger.fine("User ("+userName+"); database name = " + databaseName);
		}catch(NoSuchAlgorithmException nsae){
			logger.severe("Exception caught generating database name for user ("+userName+"); exception = " + nsae.getMessage());
			throw new BlueListProxyException("Exception caught generating database name for user ("+userName+")", nsae);
		}

		logger.exiting(CLASS_NAME, METHOD_NAME, databaseName);
		return databaseName;
    }
    
    /**
     * Create the BlueList sample database:
     *  - create database with the given name
     *  - create data type views
     * 
     * @param databaseName The BlueList sample database name
     * @throws BlueListProxyException Thrown if the database could not be created.
     */
    public static void createDatabase(String databaseName)
    throws BlueListProxyException {
		final String METHOD_NAME = "createDatabase";
		logger.entering(CLASS_NAME, METHOD_NAME, databaseName);
		
		// Create the database
		try {
			
			// See if the database currently exists
			boolean dbExists = BlueListProxyUtils.dbExists(databaseName);
			
			// Database does not already exist, create it and the views
			if ( !dbExists ) {
				
				logger.fine("Creating database: " + databaseName);
				KeyPassManager.getInstance().getAdminCloudantClient().createDB(databaseName);
				logger.fine("Database ("+databaseName+") created successfully.");
				
				addListDataTypesView(databaseName);
				
				addKeyCountTypedView(databaseName);
				
				addKeyCountUntypedView(databaseName);
				
				addCloudantQueryDataTypesIndex(databaseName);
				
			}
			
			// Database already exists, nothing to do
			else {
				logger.fine("Database ("+databaseName+") already created.");
			}
			
		} catch(BlueListProxyException blpe) {
	    	logger.severe("Failed to create database ("+databaseName+"); error = " + blpe.getMessage());
			throw blpe;
		} catch(Exception e) {
	    	logger.severe("Failed to create database ("+databaseName+"); error = " + e.getMessage());
	    	throw new BlueListProxyException("Failed to create database "+databaseName, e);
		}
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Delete BlueList sample database.
     * 
     * @param databaseName The BlueList sample database name
     * @throws BlueListProxyException Thrown if the database could not be deleted.
     */
    public static void deleteDatabase(String databaseName)
    throws BlueListProxyException {
		final String METHOD_NAME = "deleteDatabase";
		logger.entering(CLASS_NAME, METHOD_NAME, databaseName);
		
		// Delete the database
		try {
			
			// See if the database currently exists
			boolean dbExists = BlueListProxyUtils.dbExists(databaseName);
			
			if ( dbExists ) {
				
				logger.fine("Deleting database: " + databaseName);
				KeyPassManager.getInstance().getAdminCloudantClient().deleteDB(databaseName);
				logger.fine("Database ("+databaseName+") deleted successfully.");
				
			}
			
			// Database already exists, nothing to do
			else {
				logger.fine("Database ("+databaseName+") already deleted.");
			}
			
		} catch(BlueListProxyException blpe) {
	    	logger.severe("Failed to delete database ("+databaseName+"); error = " + blpe.getMessage());
			throw blpe;
		} catch(Exception e) {
	    	logger.severe("Failed to delete database ("+databaseName+"); error = " + e.getMessage());
	    	throw new BlueListProxyException("Failed to delete database "+databaseName, e);
		}
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
    }
	
    /**
     * Create list dataTypes view.
     * 
     * @param databaseName The BlueList sample database name
     * @throws BlueListProxyException Thrown if the database view could not be created.
     */
	private static void addListDataTypesView(String databaseName)
	throws BlueListProxyException {
		final String METHOD_NAME = "addListDataTypesView";
		logger.entering(CLASS_NAME, METHOD_NAME, databaseName);
		
		final String VIEW_NAME = "_design/_imfdata_listdatatypes";
		
		// Create the view
		try {
		    
		    // Create view with map and reduce functions
		    Map<String,Object> listdatatypes = new HashMap<String,Object>(2);
		    listdatatypes.put("map", "function(doc) {\n    if (doc[\"@datatype\"]) {\n        emit(doc[\"@datatype\"], 1);\n    }\n}");
		    listdatatypes.put("reduce", "_count");
		    Map<String,Object> views = new HashMap<String,Object>(1);
		    views.put("listdatatypes", listdatatypes);
		    Map<String,Object> viewddoc = new HashMap<String,Object>(2);
		    viewddoc.put("_id", VIEW_NAME);
		    viewddoc.put("views", views);
			
			logger.fine("Creating view: " + VIEW_NAME);
			Database db = KeyPassManager.getInstance().getAdminCloudantClient().database(databaseName, false);
			db.save(viewddoc);
			logger.fine("listdatatypes view for database ("+databaseName+") created successfully.");
			
		} catch(BlueListProxyException blpe) {
	    	logger.severe("Failed to create listdatatypes view for database ("+databaseName+"); error = " + blpe.getMessage());
			throw blpe;
		} catch(Exception e) {
	    	logger.severe("Failed to create listdatatypes view for database ("+databaseName+"); error = " + e.getMessage());
	    	throw new BlueListProxyException("Created database "+databaseName+" but failed to create listdatatypes view", e);
		}

		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	/**
	 * Create key count typed view.
	 * 
     * @param databaseName The BlueList sample database name
     * @throws BlueListProxyException Thrown if the database view could not be created.
	 */
	private static void addKeyCountTypedView(String databaseName)
	throws BlueListProxyException {
		final String METHOD_NAME = "addKeyCountTypedView";
		logger.entering(CLASS_NAME, METHOD_NAME, databaseName);
		
		final String VIEW_NAME = "_design/_imfdata_keycounttyped";
		
		// Create the view
		try {
		    
		    // Create view with map and reduce functions
		    Map<String,Object> keyCountTyped = new HashMap<String,Object>(2);
		    keyCountTyped.put("map", "function(doc) {\n    if (!doc.hasOwnProperty(\"@datatype\")) {\n        return;\n    }\n    var keys = Object.keys(doc);\n    for (var k in keys) {\n        key = keys[k];\n        if ([\"_id\", \"_rev\", \"@datatype\"].indexOf(key) == -1) {\n            emit([doc[\"@datatype\"], key], 1);\n        }\n    }\n}");
		    keyCountTyped.put("reduce", "_count");
		    Map<String,Object> views = new HashMap<String,Object>(1);
		    views.put("keyCountTyped", keyCountTyped);
		    Map<String,Object> viewddoc = new HashMap<String,Object>(2);
		    viewddoc.put("_id", VIEW_NAME);
		    viewddoc.put("views", views);
			
			logger.fine("Creating view: " + VIEW_NAME);
			Database db = KeyPassManager.getInstance().getAdminCloudantClient().database(databaseName, false);
			db.save(viewddoc);
			logger.fine("keyCountTyped view for database ("+databaseName+") created successfully.");
			
		} catch(BlueListProxyException blpe) {
	    	logger.severe("Failed to create keyCountTyped view for database ("+databaseName+"); error = " + blpe.getMessage());
			throw blpe;
		} catch(Exception e) {
	    	logger.severe("Failed to create keyCountTyped view for database ("+databaseName+"); error = " + e.getMessage());
	    	throw new BlueListProxyException("Created database "+databaseName+" but failed to create keyCountTyped view", e);
		}

		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	/**
	 * Create key count untyped view.
	 * 
     * @param databaseName The BlueList sample database name
     * @throws BlueListProxyException Thrown if the database view could not be created.
	 */
	private static void addKeyCountUntypedView(String databaseName)
	throws BlueListProxyException {
		final String METHOD_NAME = "addKeyCountUntypedView";
		logger.entering(CLASS_NAME, METHOD_NAME, databaseName);
		
		final String VIEW_NAME = "_design/_imfdata_keycountuntyped";
		
		// Create the view
		try {
		    
		    // Create view with map and reduce functions
		    Map<String,Object> keyCountUntyped = new HashMap<String,Object>(2);
		    keyCountUntyped.put("map", "function(doc) {\n    var keys = Object.keys(doc);\n    for (var k in keys) {\n        key = keys[k];\n        if ([\"_id\", \"_rev\", \"@datatype\"].indexOf(key) == -1) {\n            emit(key, 1);\n        }\n    }\n}");
		    keyCountUntyped.put("reduce", "_count");
		    Map<String,Object> views = new HashMap<String,Object>(1);
		    views.put("keyCountUntyped", keyCountUntyped);
		    Map<String,Object> viewddoc = new HashMap<String,Object>(2);
		    viewddoc.put("_id", VIEW_NAME);
		    viewddoc.put("views", views);
			
			logger.fine("Creating view: " + VIEW_NAME);
			Database db = KeyPassManager.getInstance().getAdminCloudantClient().database(databaseName, false);
			db.save(viewddoc);
			logger.fine("keyCountUntyped view for database ("+databaseName+") created successfully.");
			
		} catch(BlueListProxyException blpe) {
	    	logger.severe("Failed to create keyCountUntyped view for database ("+databaseName+"); error = " + blpe.getMessage());
			throw blpe;
		} catch(Exception e) {
	    	logger.severe("Failed to create keyCountUntyped view for database ("+databaseName+"); error = " + e.getMessage());
	    	throw new BlueListProxyException("Created database "+databaseName+" but failed to create keyCountUntyped view", e);
		}

		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	/**
	 * Create cloudant query @datatypes index.
	 * 
     * @param databaseName The BlueList sample database name
     * @throws BlueListProxyException Thrown if the database index could not be created.
	 */
	private static void addCloudantQueryDataTypesIndex(String databaseName)
	throws BlueListProxyException {
		final String METHOD_NAME = "addCloudantQueryDataTypesIndex";
		logger.entering(CLASS_NAME, METHOD_NAME, databaseName);
		
		final String INDEX_NAME = "_imfdata_defaultdatatype";
		
		// Create the index
		try {
		    
		    // Create view with map and reduce functions
		    IndexField[] fields = new IndexField[1];
		    fields[0] = new IndexField("@datatype", SortOrder.asc);
			
			logger.fine("Creating index: " + INDEX_NAME);
			Database db = KeyPassManager.getInstance().getAdminCloudantClient().database(databaseName, false);
			db.createIndex(null, INDEX_NAME, null, fields);
			logger.fine("@datatype index for database ("+databaseName+") created successfully.");
			
		} catch(BlueListProxyException blpe) {
	    	logger.severe("Failed to create @datatype index for database ("+databaseName+"); error = " + blpe.getMessage());
			throw blpe;
		} catch(Exception e) {
	    	logger.severe("Failed to create @datatype index for database ("+databaseName+"); error = " + e.getMessage());
	    	throw new BlueListProxyException("Created database "+databaseName+" but failed to create @datatype index", e);
		}

		logger.exiting(CLASS_NAME, METHOD_NAME);
	}

}
