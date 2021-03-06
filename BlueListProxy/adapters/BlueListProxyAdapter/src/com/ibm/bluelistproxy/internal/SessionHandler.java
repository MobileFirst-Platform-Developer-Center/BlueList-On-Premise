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
 * Handles create and delete session cookie requests.
 */
public class SessionHandler {
	private static final String CLASS_NAME = SessionHandler.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);

	
    /**
     * Session Login - obtain session cookie
     *  
     * @param request The incoming http request.
     * @return The json response.
     */
	public static JSONResponse processSessionLogin(HttpServletRequest request)
	{
		final String METHOD_NAME = "processSessionLogin";
		logger.entering(CLASS_NAME, METHOD_NAME, request);
		
		JSONResponse jsonResponse = null;
		
		try {
			
			// Get user credentials
			String userName = KeyPassManager.getInstance().getUserId(request);
			Map<String,String> userCredentials = KeyPassManager.getInstance().getUserCredentials(userName);
			
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
				
				
			} catch(BlueListProxyException blpe) {
				logger.fine("An error occurred obtaining session cookie; response error = " + blpe.getMessage());
				jsonResponse = new JSONErrorResponse(blpe);
			}
			
			
		} catch(BlueListProxyException blpe) {
			logger.fine("Unable to obtain the user credentials; response error = " + blpe.getMessage());
			jsonResponse = new JSONErrorResponse(blpe);
		}

		logger.exiting(CLASS_NAME, METHOD_NAME, jsonResponse);
		return jsonResponse;
	}

	
    /**
     * Session Logout - free session cookie
     *  
     * @param request The incoming http request.
     * @return The json response.
     */
	public static JSONResponse processSessionLogout(HttpServletRequest request)
	{
		final String METHOD_NAME = "processSessionLogout";
		logger.entering(CLASS_NAME, METHOD_NAME, request);
		
		JSONResponse jsonResponse = null;
		
		// Get user name
		String userName = KeyPassManager.getInstance().getUserId(request);
		
		try {
			
			// Delete the session cookie
			jsonResponse = SessionCookieHandler.deleteSessionCookie(userName);
			logger.fine("Session cookie cleared.");
			
		} catch(BlueListProxyException blpe) {
			logger.fine("An error occurred obtaining session cookie; response error = " + blpe.getMessage());
			jsonResponse = new JSONErrorResponse(blpe);
		}

		logger.exiting(CLASS_NAME, METHOD_NAME, jsonResponse);
		return jsonResponse;
	}

}
