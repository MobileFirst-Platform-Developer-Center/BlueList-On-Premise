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
package com.ibm.bluelistproxy;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.ibm.bluelistproxy.internal.BlueListProxyException;
import com.ibm.bluelistproxy.internal.BlueListProxyUtils;
import com.ibm.bluelistproxy.internal.EnrollHandler;
import com.ibm.bluelistproxy.internal.JSONResponse;
import com.ibm.bluelistproxy.internal.KeyPassManager;
import com.ibm.bluelistproxy.internal.SessionHandler;
import com.worklight.adapters.rest.api.WLServerAPI;
import com.worklight.adapters.rest.api.WLServerAPIProvider;
import com.worklight.core.auth.OAuthSecurity;

/**
 * Base JAX-RS adapter resource class.
 * Handles all incoming REST requests.
 */
@Path("/bluelist")
@OAuthSecurity(enabled=false)
public class BlueListProxyAdapterResource {
	private static final String CLASS_NAME = BlueListProxyAdapterResource.class.getName();
	private static final Logger logger = Logger.getLogger(CLASS_NAME);

    // Define the server api to be able to perform server operations and 
	// initialize the KeyPassManager (to read the config).
    WLServerAPI api = WLServerAPIProvider.getWLServerAPI();
    KeyPassManager kpManager = KeyPassManager.getInstance(api);

    /**
     * Handle version.
     */
	@GET
	@Produces("application/json")
	public String version(){
    	final String METHOD_NAME = "version";
    	logger.entering(CLASS_NAME, METHOD_NAME);
    	
    	String status = "unknown";
    	try {
    		KeyPassManager.getInstance().getAdminCredentials();
    		status = "ok";
    	} catch(BlueListProxyException blpe) {
    		status = "init-failed";
    	}
    	String responseString = "{\"bluelist\":\""+status+"\", \"version\": 1}";
		
    	logger.exiting(CLASS_NAME, METHOD_NAME, responseString);
    	return responseString;
	}
    
    /*
     * Handle enroll.
     */
    @PUT
    @Path("/enroll")
	@Produces("application/json")
    @OAuthSecurity(scope="cloudant")
    public Response enrollSample(@Context HttpServletRequest request) {
    	final String METHOD_NAME = "enrollSample";
    	logger.entering(CLASS_NAME, METHOD_NAME, request);
    	
    	JSONResponse jsonResponse = EnrollHandler.processEnroll(request);
    	Response response = BlueListProxyUtils.writeResponse(jsonResponse);

    	logger.exiting(CLASS_NAME, METHOD_NAME, response);
		return response;
    }
    
    /*
     * Handle un-enroll.
     */
    @DELETE
    @Path("/enroll")
	@Produces("application/json")
    @OAuthSecurity(scope="cloudant")
    public Response cleanupSample(@Context HttpServletRequest request) {
    	final String METHOD_NAME = "cleanupSample";
    	logger.entering(CLASS_NAME, METHOD_NAME, request);
    	
    	JSONResponse jsonResponse = EnrollHandler.processCleanup(request);
    	Response response = BlueListProxyUtils.writeResponse(jsonResponse);

    	logger.exiting(CLASS_NAME, METHOD_NAME, response);
		return response;
    }
    
    /*
     * Handle session cookie.
     */
    @POST
    @Path("/sessioncookie")
	@Produces("application/json")
    @OAuthSecurity(scope="cloudant")
    public Response sessionLogin(@Context HttpServletRequest request) {
    	final String METHOD_NAME = "sessionLogin";
    	logger.entering(CLASS_NAME, METHOD_NAME, request);
    	
    	JSONResponse jsonResponse = SessionHandler.processSessionLogin(request);
    	Response response = BlueListProxyUtils.writeResponse(jsonResponse);

    	logger.exiting(CLASS_NAME, METHOD_NAME, response);
		return response;
    }
    
    /*
     * Handle delete session cookie.
     */
    @DELETE
    @Path("/sessioncookie")
	@Produces("application/json")
    @OAuthSecurity(scope="cloudant")
    public Response sessionLogout(@Context HttpServletRequest request) {
    	final String METHOD_NAME = "sessionLogout";
    	logger.entering(CLASS_NAME, METHOD_NAME, request);
    	
    	JSONResponse jsonResponse = SessionHandler.processSessionLogout(request);
    	Response response = BlueListProxyUtils.writeResponse(jsonResponse);

    	logger.exiting(CLASS_NAME, METHOD_NAME, response);
		return response;
    }
		
}
