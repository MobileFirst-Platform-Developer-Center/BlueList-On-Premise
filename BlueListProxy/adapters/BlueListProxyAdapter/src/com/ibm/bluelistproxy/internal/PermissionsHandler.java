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

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Hex;

import com.cloudant.client.api.Database;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

/**
 * Processes all database permissions related requests.
 * This includes set permissions (create _users database, add _users database entry for user,
 * update database permissions for user) and remove permissions (update database permissions for user
 * and remove _users database entry for user).
 */
public class PermissionsHandler {
	private static final String CLASS_NAME = PermissionsHandler.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);
    
    
    /**
     * Update permissions for user for this database:
     *  - if _users database does not exist create it and the view
     *  - if entry for user does not exist in _users database, add user
     *  - if user does not have admins permissions for database, add the permissions
     *  
     * @param userName The user name
     * @param databaseName The database name
     * @throws BlueListProxyException Thrown if the permissions could not be set.
     */
    public static void setPermissions(String userName, String databaseName)
    throws BlueListProxyException {
		final String METHOD_NAME = "setPermissions";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {userName, databaseName});
		
		// Start with creating the _users database and proceed to updating the permissions
	    createUsersDatabase(userName, databaseName);
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
    } 
    
    /**
     * Remove permissions for user for this database:
     *  - clear admins permissions for database
     *  - remove user from _users database
     *  - clear user from user credentials cache
     * 
     * @param userName The user name
     * @param databaseName The database name
     * @throws BlueListProxyException Thrown if the permissions could not be removed.
     */
    public static void removePermissions(String userName, String databaseName)
    throws BlueListProxyException {
		final String METHOD_NAME = "removePermissions";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {userName, databaseName});
		
		// Initialize flags/exceptions for cleanup in case of error
		BlueListProxyException rootException = null;
		
		try {
			
			// Remove this user's access to the database
			removeUserAccess(userName, databaseName);
			
		} catch(BlueListProxyException blpe) {
			logger.fine("An error occurred removing access to database("+databaseName+") for user ("+userName+"); response error = " + blpe.getMessage());
			rootException = blpe;
		}
		
		try {
			
			// Delete the user from the user's database
			deleteUserEntry(userName, databaseName);
			
		} catch(BlueListProxyException blpe) {
			logger.fine("An error occurred removing user ("+userName+") from the _users database; response error = " + blpe.getMessage());
			if (rootException == null) rootException = blpe;
		}
		
		// Clear the user credentials from the cache
		KeyPassManager.getInstance().clearCredentials(userName);
		
		// If there was an exception, throw the original exception now
		if (rootException != null) {
			throw rootException;
		}
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
    }   
    
    /**
     * Create the _users database.
     * 
     * @param userName The user name
     * @param databaseName The database name
     * @throws BlueListProxyException Thrown if the _users database could not be created.
     */
    public static void createUsersDatabase(String userName, String databaseName)
    throws BlueListProxyException {
		final String METHOD_NAME = "createUsersDatabase";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {userName, databaseName});
		
		// See if the _users database currently exists
		boolean dbExists = BlueListProxyUtils.dbExists("_users");
		
		// If the database does not exist, create it and add the view
		if ( !dbExists ) {
			
			// Create the _users database
			try {
				
				logger.fine("Creating database: _users");
				KeyPassManager.getInstance().getAdminCloudantClient().createDB("_users");
				logger.fine("Created _users database; adding view");
				
				// Create the _users database view
				createUsersDatabaseView(userName, databaseName);
				
			} catch(BlueListProxyException blpe) {
		    	logger.severe("Failed to create database (_users); error = " + blpe.getMessage());
				throw blpe;
			} catch(Exception e) {
		    	logger.severe("Failed to create database (_users); error = " + e.getMessage());
		    	throw new BlueListProxyException("Failed to create database (_users)", e);
			}
			
		}
		
		// If the database does exist, add/update user
		else {
			
			// Add/update user
			boolean userDocExists = BlueListProxyUtils.dbDocExists("_users", "org.couchdb.user:" + userName);
			
			// If the _users document already exists, update permissions
			if (userDocExists) {
				
				logger.fine("User ("+userName+") _users database info exists; Updating access");
				addUserAccess(userName, databaseName);
				
			}
			
			// If the _users document does not exist, create it
			else {

				logger.fine("User ("+userName+") _users database info does not exist; Creating it and adding access");
				createUserEntry(userName, databaseName);
				
			}
			
		}
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Create the _users database view.
     * 
     * @param userName The user name
     * @param databaseName The database name
     * @throws BlueListProxyException Thrown if the _users database view could not be created.
     */
    public static void createUsersDatabaseView(String userName, String databaseName)
    throws BlueListProxyException {
		final String METHOD_NAME = "createUsersDatabaseView";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {userName, databaseName});
		
		final String VIEW_NAME = "_design/_imfdata_usersview";
		
		// Create the view
		try {
		    
		    // Create view with map function
		    Map<String,Object> users = new HashMap<String,Object>(1);
		    users.put("map", "function(doc) {\n  emit(doc._id, doc);\n}");
		    Map<String,Object> views = new HashMap<String,Object>(1);
		    views.put("users", users);
		    Map<String,Object> viewddoc = new HashMap<String,Object>(2);
		    viewddoc.put("_id", VIEW_NAME);
		    viewddoc.put("views", views);
			
			logger.fine("Creating _users view: " + VIEW_NAME);
			Database db = KeyPassManager.getInstance().getAdminCloudantClient().database("_users", false);
			db.save(viewddoc);
			logger.fine("Created _users database view");
			
			// Create the user entry
			createUserEntry(userName, databaseName);
			
		} catch(BlueListProxyException blpe) {
	    	logger.severe("Failed to create users view for database (_users); error = " + blpe.getMessage());
			throw blpe;
		} catch(Exception e) {
	    	logger.severe("Failed to create users view for database (_users); error = " + e.getMessage());
	    	throw new BlueListProxyException("Failed to create users view for database (_users)", e);
		}
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Create _users database entry for specific user.
     * 
     * @param userName The user name
     * @param databaseName The database name
     * @throws BlueListProxyException Thrown if the _users database entry could not be created.
     */
    public static void createUserEntry(String userName, String databaseName)
    throws BlueListProxyException {
		final String METHOD_NAME = "createUserEntry";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {userName, databaseName});
		
		final String DOC_NAME = "org.couchdb.user:" + userName;

		// Generate password, salt, and encrypted password
		byte[] randomBytes = new byte[8];
		new Random().nextBytes(randomBytes);
		String password = Hex.encodeHexString(randomBytes);
		new Random().nextBytes(randomBytes);
		String salt = Hex.encodeHexString(randomBytes);
		
		String encryptedPass_hex;
		MessageDigest md;
		try{
			
			encryptedPass_hex = KeyPassManager.getInstance().getCryptoUtil().encrypt(password, salt);
			
			md = MessageDigest.getInstance("SHA1");
			md.update((password+salt).getBytes());
			
		} catch(NoSuchAlgorithmException nsae) {
			logger.severe("Exception caught generating password for user ("+userName+"); exception = " + nsae.getMessage());
			throw new BlueListProxyException("Exception caught generating password for user ("+userName+")", nsae);
		}
		
		byte [] password_sha = md.digest();
		String password_sha_hex = Hex.encodeHexString(password_sha);
		
		// Create request body
		JSONObject body_credentials = new JSONObject();
		body_credentials.put("_id", DOC_NAME);
		body_credentials.put("name", userName);
		body_credentials.put("password", encryptedPass_hex);
		body_credentials.put("salt", salt);
		body_credentials.put("password_sha", password_sha_hex);
		body_credentials.put("roles", new JSONArray());
		body_credentials.put("type", "user");
		
		// Create the _users document
		try {
			
			// Create _users document
			logger.fine("Creating _users document: " + DOC_NAME);
			Database db = KeyPassManager.getInstance().getAdminCloudantClient().database("_users", false);
			db.save(body_credentials);
			
			// Add permissions
			logger.fine("_users database document for user ("+userName+") created; Adding access.");
			addUserAccess(userName, databaseName);
			
		} catch(BlueListProxyException blpe) {
	    	logger.severe("Failed to create database document (_users/org.couchdb.user:"+userName+"); error = " + blpe.getMessage());
			throw blpe;
		} catch(Exception e) {
	    	logger.severe("Failed to create database document (_users/org.couchdb.user:"+userName+"); error = " + e.getMessage());
	    	throw new BlueListProxyException("Failed to create database document (_users/org.couchdb.user:"+userName+")", e);
		}
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Update user permissions for database.
     * 
     * @param userName The user name
     * @param databaseName The database name
     * @throws BlueListProxyException Thrown if the database permissions could not be updated.
     */
    public static void addUserAccess(String userName, String databaseName)
    throws BlueListProxyException {
		final String METHOD_NAME = "addUserAccess";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {userName, databaseName});
		
		final String DOC_NAME = "_security";
		
		// Obtain the database security document; it should be there
		InputStream jsonStream = null;
		try {
			
			// Get security document
			logger.fine("Retrieving _security document for database " + databaseName);
			Database db = KeyPassManager.getInstance().getAdminCloudantClient().database(databaseName, false);
			jsonStream = db.find(DOC_NAME);
			JSONObject jsonBody = JSONObject.parse(jsonStream);
			
			// Determine if the security document for this database already exists
			boolean existingSecurityDoc = false;
			if (jsonBody.containsKey("couchdb_auth_only") || 
				jsonBody.containsKey("admins") || 
				jsonBody.containsKey("members")) { 
				existingSecurityDoc = true;
			}
			
			// Update security admins info for this user to give admins access
			boolean existingMember = false;
			jsonBody.put("couchdb_auth_only", true);
			JSONObject admins = (JSONObject)jsonBody.get("admins");
			if (admins == null){
				admins = new JSONObject();
				JSONArray namesArray = new JSONArray();
				namesArray.add(userName);
				admins.put("names", namesArray);
				jsonBody.put("admins", admins);
			}
			else {
				JSONArray namesArray = (JSONArray)admins.get("names");
				if (namesArray != null) {
					existingMember = namesArray.contains(userName);
					if (existingMember == false) {
						namesArray.add(userName);
					}
				}
				else {
					namesArray = new JSONArray();
					namesArray.add(userName);
					admins.put("names", namesArray);
				}
			}
			
			// If member does not already exist, then update the permissions
			if (existingMember == false) {
				JSONObject members = (JSONObject)jsonBody.get("members");
				
				// Update security members info for this user to give admins access
				if (members == null  &&  existingSecurityDoc == false) {
					JSONArray namesArray = new JSONArray();
					JSONArray rolesArray = new JSONArray();
					rolesArray.add("_admin");
					members = new JSONObject();
					members.put("names", namesArray);
					members.put("roles", rolesArray);
					jsonBody.put("members", members);
				}
				
				Object idObj = jsonBody.get("_id");
				if (!(idObj instanceof String)) jsonBody.put("_id", DOC_NAME);

				// Store the updated document
				logger.fine("Setting permissions for database: " + databaseName);
				if (existingSecurityDoc) db.update(jsonBody);
				else db.save(jsonBody);
				logger.fine("Permissions for user ("+userName+") and database ("+databaseName+") set successfully.");
				
				
			}
			
			// User already exists as member
			else {
				logger.fine("Permissions for user ("+userName+") and database ("+databaseName+") already exist; nothing more to do");
			}
			
		} catch(BlueListProxyException blpe) {
	    	logger.severe("Failed to set permissions for database ("+databaseName+"); error = " + blpe.getMessage());
			throw blpe;
		} catch(Exception e) {
	    	logger.severe("Failed to set permissions for database ("+databaseName+"); error = " + e.getMessage());
	    	throw new BlueListProxyException("Failed to set database "+databaseName+" permissions", e);
		} finally {
			if (jsonStream != null) {
				try { 
					jsonStream.close(); 
				} catch(Exception e) {}
			}
		}
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Delete _users database entry for specific user.
     * 
     * @param userName The user name
     * @param databaseName The database name
     * @throws BlueListProxyException Thrown if the _users database entry could not be deleted.
     */
    public static void deleteUserEntry(String userName, String databaseName)
    throws BlueListProxyException {
		final String METHOD_NAME = "deleteUserEntry";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {userName, databaseName});
		
		final String DOC_NAME = "org.couchdb.user:" + userName;
		
		// Get user credentials
		InputStream jsonStream = null;
		try {
			
			// See if _users document exists
			if ( BlueListProxyUtils.dbDocExists("_users", DOC_NAME) ) {
				
				logger.fine("Retrieving _users document: " + DOC_NAME);
				Database db = KeyPassManager.getInstance().getAdminCloudantClient().database("_users", false);
				jsonStream = db.find(DOC_NAME);
				JSONObject jsonBody = JSONObject.parse(jsonStream);
				logger.fine("_users database document for user ("+userName+") retrieved; removing it.");
				
				db.remove(jsonBody);
				logger.fine("_users database entry for user ("+userName+") deleted");
				
			}
			
			else {
				logger.fine("_users database entry for user ("+userName+") does not exist; nothing more to do");
			}
			
		} catch(BlueListProxyException blpe) {
	    	logger.severe("Failed to delete _users database document ("+DOC_NAME+"); error = " + blpe.getMessage());
			throw blpe;
		} catch(Exception e) {
	    	logger.severe("Failed to delete _users database document ("+DOC_NAME+"); error = " + e.getMessage());
	    	throw new BlueListProxyException("Failed to delete _users database document ("+DOC_NAME+")", e);
		} finally {
			if (jsonStream != null) {
				try { 
					jsonStream.close(); 
				} catch(Exception e) {}
			}
		}
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Remove user permissions for database.
     * 
     * @param userName The user name
     * @param databaseName The database name
     * @throws BlueListProxyException Thrown if the database permissions could not be removed.
     */
    public static void removeUserAccess(String userName, String databaseName)
    throws BlueListProxyException {
		final String METHOD_NAME = "removeUserAccess";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {userName, databaseName});
		
		final String DOC_NAME = "_security";
		
		// Obtain the database security document; it should be there
		InputStream jsonStream = null;
		try {
			
			// See if _security document exists
			if ( BlueListProxyUtils.dbDocExists(databaseName, DOC_NAME) ) {
				
				// Get security document
				logger.fine("Retrieving _security document for database " + databaseName);
				Database db = KeyPassManager.getInstance().getAdminCloudantClient().database(databaseName, false);
				jsonStream = db.find(DOC_NAME);
				JSONObject jsonBody = JSONObject.parse(jsonStream);
				
				// Update security admins info for this user to give admins access
				boolean existingMember = false;
				JSONObject admins = (JSONObject)jsonBody.get("admins");
				if (admins != null){
					JSONArray namesArray = (JSONArray)admins.get("names");
					existingMember = namesArray.contains(userName);
					if (existingMember == true) {
						namesArray.remove(userName);
					}
				}
				
				// If member exists, then update the permissions
				if (existingMember == true) {

					Object idObj = jsonBody.get("_id");
					if (!(idObj instanceof String)) jsonBody.put("_id", DOC_NAME);
					Object revObj = jsonBody.get("_rev");
					

					// Store the updated document
					logger.fine("Updating _security document for database: " + databaseName);
					if (revObj instanceof String) db.update(jsonBody);
					else db.save(jsonBody);
					logger.fine("Permissions for user ("+userName+") and database ("+databaseName+") updated successfully.");
					
				}
				
				// User already does not exist as member
				else {
					logger.fine("Permissions for user ("+userName+") and database ("+databaseName+") does not exist; nothing more to do");
				}
			}
			
			// User already does not exist as member
			else {
				logger.fine("Permissions for user ("+userName+") and database ("+databaseName+") do not exist; nothing more to do");
			}
			
		} catch(BlueListProxyException blpe) {
	    	logger.severe("Failed to remove permissions for database ("+databaseName+"); error = " + blpe.getMessage());
			throw blpe;
		} catch(Exception e) {
	    	logger.severe("Failed to remove permissions for database ("+databaseName+"); error = " + e.getMessage());
	    	throw new BlueListProxyException("Failed to remove database "+databaseName+" permissions", e);
		} finally {
			if (jsonStream != null) {
				try { 
					jsonStream.close(); 
				} catch(Exception e) {}
			}
		}
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
    }

}
