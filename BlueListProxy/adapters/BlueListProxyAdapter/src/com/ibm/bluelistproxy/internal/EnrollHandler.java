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

import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.ibm.json.java.JSONObject;

/**
 * Handles enroll and un-enroll requests.
 */
public class EnrollHandler {
	private static final String CLASS_NAME = EnrollHandler.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);
	
    /**
     * Handle bluelist sample enroll:
     *  - create database with generated name
     *  - set permissions for given user
     *  - create session cookie for given user
     *  
     * @param request The incoming http request.
     * @return The json response.
     */
	public static JSONResponse processEnroll(HttpServletRequest request)
	{
		final String METHOD_NAME = "processEnroll";
		logger.entering(CLASS_NAME, METHOD_NAME, request);
		
		JSONResponse jsonResponse = null;
		
		// Enclose everything in try block; all exceptions will be converted to a JSONErrorResponse
		try {
			
			// Initialize flags/exceptions for cleanup in case of error
			boolean databaseError = false;
			boolean permissionsError = false;
			boolean sessioncookieError = false;
			BlueListProxyException rootException = null;
			
			// Get admin credentials
			Map<String,String> userCredentials = null;
			
			// Get user name
			String userName = KeyPassManager.getInstance().getUserId(request);
			
			// Get database name based on userId
			String databaseName = DatabaseHandler.getDatabaseName(userName);
			
			try {
				
				// Create the database, views, and @datatype index
				DatabaseHandler.createDatabase(databaseName);
				
				try {
					
					// Set permissions
					PermissionsHandler.setPermissions(userName, databaseName);
					
					try {
						
						// Get user credentials
						userCredentials = KeyPassManager.getInstance().getUserCredentials(userName);
						
						try {
							
							// This request is only valid if https is being used end-to-end.
	                        // If the incoming request is over http, then return an error indicating this.
	                        // If the outgoing request is over http, then return an error indicating this.
							if ("http".equals(request.getProtocol())) {
								logger.warning("Incoming protocol ("+request.getProtocol()+") from client is not https; the session cookie may not pass through.");
							}
							else if ("http".equals(userCredentials.get("protocol"))) {
								logger.warning("Outgoing protocol ("+userCredentials.get("protocol")+") to cloudant is not https; the session cookie may not pass through.");
							}
							
							// Create the session cookie
							jsonResponse = SessionCookieHandler.createSessionCookie(userName);
							
							// Update cloudant response before forwarding back
							JSONObject jsonBody = jsonResponse.getResult();
							
							// Add cloudant access information to response body
							JSONObject cloudantAccess = new JSONObject();
							cloudantAccess.put("protocol", userCredentials.get("protocol"));
							cloudantAccess.put("host", userCredentials.get("host"));
							cloudantAccess.put("port", userCredentials.get("port"));
							jsonBody.put("cloudant_access", cloudantAccess);
							
							// Add generated database name to response body
							jsonBody.put("database", databaseName);
							
							
						} catch(BlueListProxyException blpe) {
							logger.fine("An error occurred obtaining session cookie for user ("+userName+"); response error = " + blpe.getMessage());
							sessioncookieError = true;
							rootException = blpe;
						}
						
					} catch(BlueListProxyException blpe) {
						logger.fine("Unable to obtain the user credentials; response error = " + blpe.getMessage());
						permissionsError = true;
						rootException = blpe;
					}
					
					
				} catch(BlueListProxyException blpe) {
					logger.fine("An error occurred setting permissions for user ("+userName+") for database("+databaseName+"); response error = " + blpe.getMessage());
					permissionsError = true;
					rootException = blpe;
				}
				
			} catch(BlueListProxyException blpe) {
				logger.fine("An error occurred creating database("+databaseName+"); response error = " + blpe.getMessage());
				databaseError = true;
				rootException = blpe;
			}
			
			// Handle no error case, return session cookie payload
			if (databaseError == false  &&  permissionsError == false  &&  sessioncookieError == false) {
				logger.fine("Enroll processed successfully.");
			}
			
			// Handle cleanup due to errors
			else {
				
				// If permissions or session cookie error
				if (permissionsError  ||  sessioncookieError) {
					
					// If session cookie error; attempt to cleanup session cookie
					if (sessioncookieError) {
						
						try {
							
							// Delete the session cookie
							SessionCookieHandler.deleteSessionCookie(userName);
							logger.fine("Session cookie cleared for user ("+userName+").");
							
						} catch(BlueListProxyException blpe) {
							logger.fine("Error occurred clearing session cookie for user ("+userName+"); response error = " + blpe.getMessage());
						}
						
					}
					
					try {
						
						// Remove the permissions
						PermissionsHandler.removePermissions(userName, databaseName);
						logger.fine("Permissions removed for user ("+userName+") for database ("+databaseName+").");
						
					} catch(BlueListProxyException blpe) {
						logger.fine("Error detected removing the permissions; response error = " + blpe.getMessage());
					}
					
				}
				
				try {
					
					// Delete the database
					DatabaseHandler.deleteDatabase(databaseName);
					logger.fine("Deleted database ("+databaseName+").");
					
				} catch(BlueListProxyException blpe) {
					logger.fine("Error occurred deleting database ("+databaseName+"); response error = " + blpe.getMessage());
				}
				
				// Return response associated with root exception
				jsonResponse = new JSONErrorResponse(rootException);
				
			}
			
			
		}
		
		// Handle exceptions
		catch(BlueListProxyException blpe) {
			jsonResponse = new JSONErrorResponse(blpe);
		}

		logger.exiting(CLASS_NAME, METHOD_NAME, jsonResponse);
		return jsonResponse;
	}

	
    /**
     * Handle bluelist sample un-enroll (cleanup):
     *  - delete session cookie for given user
     *  - remove permissions for given user
     *  - delete database with generated name
     *  
     * @param request The incoming http request.
     * @return The json response.
     */
	public static JSONResponse processCleanup(HttpServletRequest request)
	{
		final String METHOD_NAME = "processCleanup";
		logger.entering(CLASS_NAME, METHOD_NAME, request);
		
		JSONResponse jsonResponse = null;
		
		// Enclose everything in try block; all exceptions will be converted to a JSONErrorResponse
		try {
			
			// Initialize flags/exceptions for cleanup in case of error
			boolean errorFlag = false;
			BlueListProxyException rootException = null;
			
			// Get user name
			String userName = KeyPassManager.getInstance().getUserId(request);
			
			// Get database name based on userId
			String databaseName = DatabaseHandler.getDatabaseName(userName);
			
			try {
				
				// Delete the session cookie
				jsonResponse = SessionCookieHandler.deleteSessionCookie(userName);
				logger.fine("Session cookie for user ("+userName+") cleared.");
				
			} catch(BlueListProxyException blpe) {
				logger.fine("Error occurred clearing session cookie for user ("+userName+"); response error = " + blpe.getMessage());
				errorFlag = true;
				rootException = blpe;
			}
			
			// Handle cleaning permissions
			try {
				
				// Remove the permissions
				PermissionsHandler.removePermissions(userName, databaseName);
				logger.fine("Permissions removed for user ("+userName+") for database ("+databaseName+").");
				
			} catch(BlueListProxyException blpe) {
				logger.fine("Error occurred removing permissions for user ("+userName+") for database ("+databaseName+"); response error = " + blpe.getMessage());
				errorFlag = true;
				if (rootException == null) rootException = blpe;
			}
			
			// Handle cleaning database
			try {
				
				// Delete the database
				DatabaseHandler.deleteDatabase(databaseName);
				logger.fine("Deleted database ("+databaseName+").");
				
			} catch(BlueListProxyException blpe) {
				logger.fine("Error occured deleting database ("+databaseName+"); response error = " + blpe.getMessage());
				errorFlag = true;
				if (rootException == null) rootException = blpe;
			}
			
			// Handle no error case, return session cookie payload
			if (errorFlag == false) {
				
				logger.fine("Cleanup processed successfully.");
				
			}
			
			// Handle cleanup due to errors
			else {
				
				// Return response associated with root exception
				jsonResponse = new JSONErrorResponse(rootException);
				
			}
			
		}
		
		// Handle exceptions
		catch(BlueListProxyException blpe) {
			jsonResponse = new JSONErrorResponse(blpe);
		}

		logger.exiting(CLASS_NAME, METHOD_NAME, jsonResponse);
		return jsonResponse;
	}

}
