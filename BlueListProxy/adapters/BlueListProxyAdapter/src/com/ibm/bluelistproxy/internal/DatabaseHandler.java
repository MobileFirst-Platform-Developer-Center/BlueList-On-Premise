/**
* Copyright 2015 IBM Corp.
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
package com.ibm.bluelistproxy.internal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Hex;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.IndexField;
import com.cloudant.client.api.model.IndexField.SortOrder;

/**
 * Processes all database related requests.
 * This includes create database (along with index) and delete database.
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
     *  - create @datatype index
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
			
			// Database does not already exist, create it and the index
			if ( !dbExists ) {
				
				logger.fine("Creating database: " + databaseName);
				KeyPassManager.getInstance().getAdminCloudantClient().createDB(databaseName);
				logger.fine("Database ("+databaseName+") created successfully.");
				
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
		    
		    // Create @datatype index
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
