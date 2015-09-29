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

import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.DbInfo;

/**
 * BlueList proxy utility class.  The utilities are related to sending requests and
 * writing responses.
 */
public class BlueListProxyUtils {
	private static final String CLASS_NAME = BlueListProxyUtils.class.getName();
	private static final Logger logger = Logger.getLogger(CLASS_NAME);
	
	/**
	 * Determines whether the current database already exists.
	 * This is done my making a request that involves the database.
	 * 
	 * @param databaseName The name of the database.
	 * @return True if it exists, false otherwise.
	 */
	public static boolean dbExists(String databaseName) {
		final String METHOD_NAME = "dbExists";
		logger.entering(CLASS_NAME, METHOD_NAME, databaseName);
		
		boolean dbExists = false;
		try {
			Database db = KeyPassManager.getInstance().getAdminCloudantClient().database(databaseName, false);
			DbInfo dbInfo = db.info();
			dbExists = ( dbInfo != null );
		} catch(Exception e) {}

		logger.exiting(CLASS_NAME, METHOD_NAME, dbExists);
		return dbExists;
	}
	
	/**
	 * Determines whether the current database document already exists.
	 * 
	 * @param databaseName The name of the database.
	 * @param documentName The name of the database document.
	 * @return True if it exists, false otherwise.
	 */
	public static boolean dbDocExists(String databaseName, String documentName) {
		final String METHOD_NAME = "dbDocExists";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {databaseName, documentName});
		
		boolean dbDocExists = false;
		try {
			Database db = KeyPassManager.getInstance().getAdminCloudantClient().database(databaseName, false);
			dbDocExists = db.contains(documentName);
		} catch(Exception e) {}

		logger.exiting(CLASS_NAME, METHOD_NAME, dbDocExists);
		return dbDocExists;
	}
	
	/**
	 * Create a JAX-rs response from the given json response.
	 * 
	 * @param jsonResponse The json response to write.
	 * @return The JAX-rs response.
	 */
	public static Response writeResponse(JSONResponse jsonResponse) {
		final String METHOD_NAME = "writeResponse";
		logger.entering(CLASS_NAME, METHOD_NAME, jsonResponse);
		
		// Create response builder and set http status code
		int statusCode = jsonResponse.getStatusCode();
		ResponseBuilder rb = Response.status(statusCode);
		
		// Add response headers (copied from Cloudant response)
		Map<String,String> hdrs = jsonResponse.getHeaders();
		if (hdrs != null) {
			for (Map.Entry<String,String> hdr : hdrs.entrySet()) {
				rb.header(hdr.getKey(), hdr.getValue());
			}
		}
		
		// Copy payload
		Response response = rb.entity(jsonResponse.getResult()).type(jsonResponse.getContentType()).build();

		logger.exiting(CLASS_NAME, METHOD_NAME, response);
		return response;
	}

}
