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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.ibm.json.java.JSONObject;


/**
 * Processes all session cookie related requests.
 * This includes obtaining a session cookie and deleting a session cookie.
 */
public class SessionCookieHandler {
	private static final String CLASS_NAME = SessionCookieHandler.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);   
    
    /**
     * Obtain and return the session cookie on behalf of user.
     * The response is the cloudant response.
     * 
     * @param userName The user name.
     * @return The response from session cookie request to cloudant.
     * @throws BlueListProxyException Thrown if the session cookie could not be obtained.
     */
    public static JSONResponse createSessionCookie(String userName)
    throws BlueListProxyException {
		final String METHOD_NAME = "createSessionCookie";
		logger.entering(CLASS_NAME, METHOD_NAME, userName);
		
		JSONResponse jsonResponse = null;
		
		// Create the session cookie
		try {
			
			logger.fine("Creating session cookie");
			jsonResponse = sendCookieRequest(userName);
			logger.fine("Session cookie for user ("+userName+") created successfully.");
			
		} catch(BlueListProxyException blpe) {
	    	logger.severe("Failed to create session cookie for user ("+userName+"); error = " + blpe.getMessage());
			throw blpe;
		} catch(Exception e) {
	    	logger.severe("Failed to create session cookie for user ("+userName+"); error = " + e.getMessage());
	    	throw new BlueListProxyException("Failed to create session cookie for user ("+userName+")", e);
		}
		
		logger.exiting(CLASS_NAME, METHOD_NAME, jsonResponse);
		return jsonResponse;
    }
    
    /**
     * Delete the session cookie on behalf of user.
     * 
     * @param userName The user name.
     * @return The dummy response.
     * @throws BlueListProxyException Thrown if the session cookie could not be deleted.
     */
    public static JSONResponse deleteSessionCookie(String userName)
    throws BlueListProxyException {
		final String METHOD_NAME = "deleteSessionCookie";
		logger.entering(CLASS_NAME, METHOD_NAME, userName);
		
		// Create dummy payload to return
		JSONObject jsonPayload = new JSONObject();
		jsonPayload.put("ok", true);
		
		// Create headers
		Map<String,String> headers = new HashMap<String, String>(1);
		headers.put("Content-Type", "application/json");
		
		// There is no api to delete a session cookie; do nothing
		JSONResponse jsonResponse = new JSONResponse(200, headers, jsonPayload);
		
		logger.exiting(CLASS_NAME, METHOD_NAME, jsonResponse);
		return jsonResponse;
    }
	
	/**
	 * Sends the given http request to obtain session cookie.
	 * 
	 * @param userName The user to obtain session cookie for.
	 * @return The received JSON response.
	 * @throws BlueListProxyException If an error occurred sending the request or processing the response.
	 */
	private static JSONResponse sendCookieRequest(String userName)
	throws BlueListProxyException {
		final String METHOD_NAME = "sendCookieRequest";
		logger.entering(CLASS_NAME, METHOD_NAME, userName);
		
		JSONResponse jsonResponse = null;
		
		Map<String,String> userCredentials = KeyPassManager.getInstance().getUserCredentials(userName);

		// Create credentials provider
		CredentialsProvider creds = new BasicCredentialsProvider();
		creds.setCredentials(new AuthScope(userCredentials.get("host"), Integer.parseInt(userCredentials.get("port"))),
				             new UsernamePasswordCredentials(userCredentials.get("username"), userCredentials.get("password")));
		
		HttpClient httpClient = null;

		// Send the request and gather the response code.
		try {
			
			// Create request
			String url = userCredentials.get("protocol") + "://" + userCredentials.get("host") + ":" + userCredentials.get("port") + "/_session";
			HttpPost httpRequest = new HttpPost(url);
    		
    		// Build headers
    		httpRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
    		httpRequest.addHeader("Accept", "application/json");
    		
    		// Create the payload entity with the form parameters
    		List<NameValuePair> formPairs = new ArrayList<NameValuePair>(2);
			formPairs.add(new BasicNameValuePair("name", userCredentials.get("username")));
			formPairs.add(new BasicNameValuePair("password", userCredentials.get("password")));
    		UrlEncodedFormEntity payloadEntity = null;
    		try {
    			payloadEntity = new UrlEncodedFormEntity(formPairs, "UTF-8");
    		} catch(Exception e) {}
    		payloadEntity.setContentType("application/x-www-form-urlencoded");
    		httpRequest.setEntity(payloadEntity);

    		// Issue the http request
        	httpClient = HttpClients.createDefault();
    		HttpResponse httpResponse = httpClient.execute(httpRequest);
        	
        	// Validate the http response
			if (httpResponse != null) {
				
				try {
					
					// Obtain http response code
					int responseCode = (httpResponse.getStatusLine() != null ? httpResponse.getStatusLine().getStatusCode() : 0);
					
					// Create json response from http response
					if ((httpResponse.getEntity() != null)  &&  (httpResponse.getEntity().getContent() != null)) {
						InputStream inputStream = httpResponse.getEntity().getContent();
						String contentType = httpResponse.getEntity().getContentType().getValue();

						// Make sure the content is json
						if (MediaType.APPLICATION_JSON.toString().equals(contentType)) {
							
							try {

								// Get JSON respresentation of response payload (or null)
								JSONObject jsonPayload = null;
								StringWriter stringWriter = new StringWriter();
								IOUtils.copy(inputStream, stringWriter, "UTF-8");
								String responseString = stringWriter.toString();
				    			if (responseString != null && responseString.length() > 0) {
									try {
										jsonPayload = (JSONObject)JSONObject.parse(responseString);
									} catch(IOException ioe) {
										throw new BlueListProxyException("Error converting http response to JSON object", ioe);
									}
								}
								
								// Create JSON response from response code, headers, and json payload.
				    			// Put session cookie in headers and payload.
				    			Map<String,String> headers = new HashMap<String, String>(2);
				    			headers.put("Content-Type", "application/json");
				    			String sessionCookie = getSessionCookieFromHeaders(httpResponse.getAllHeaders());
				    			if (sessionCookie != null) {
				    				headers.put("cookie", sessionCookie);
					    			jsonPayload.put("sessionCookie", sessionCookie);
				    			}
				    			if (sessionCookie != null) 
								jsonResponse = new JSONResponse(responseCode, headers, jsonPayload);
								logger.fine("response:" + jsonResponse);
				    			
							} catch(IOException ioe) {
								throw new BlueListProxyException("Error reading http response payload", ioe);
							}
							finally {
								if (inputStream != null) {
									try {
										inputStream.close();
									} catch(IOException ioe) {}
								} 
							}
						} else {
							throw new BlueListProxyException("Unexpected content type ("+httpResponse.getEntity().getContentType().getValue()+") is not application/json");
						}
					} else {
						throw new BlueListProxyException("Unexpected missing JSON response.");
					}
					
				} catch(IOException ioe) {
					throw new BlueListProxyException("Error processing http response", ioe);
				}
				finally {
					if (httpResponse instanceof CloseableHttpResponse) {
						try {
							((CloseableHttpResponse)httpResponse).close();
						} catch(IOException ioe) {}
					} 
				}
			} else {
				throw new BlueListProxyException("No http response received");
			}

		} catch (IOException ioe) {
			throw new BlueListProxyException("Error sending http request", ioe);
		} finally {
			if (httpClient instanceof CloseableHttpClient) {
				try {
					((CloseableHttpClient)httpClient).close();
				} catch(IOException ioe) {}
			} 
		}

		return jsonResponse;
	}
    
    /**
     * Returns the AuthSession cookie from the headers or
     * null if not found.
     * 
     * @param headers The request or response headers
     * @return The AuthSession cookie
     */
    private static String getSessionCookieFromHeaders(Header[] headers) {
		final String METHOD_NAME = "getSessionCookieFromHeaders";
		logger.entering(CLASS_NAME, METHOD_NAME, headers);
		
		String sessionCookie = null;
		
		// Search cookie headers for an authsession cookie
		if (headers != null) {
			for (Header header : headers) {
				if ((header.getName().toLowerCase().contains("set-cookie"))  &&
					 (header.getValue().toLowerCase().startsWith("authsession"))) {
					sessionCookie = header.getValue();
					break;
				}
			}
		}
		
		logger.fine("Session cookie " + (sessionCookie == null ? "not " : "") + "found");
		
		logger.exiting(CLASS_NAME, METHOD_NAME,sessionCookie);
		return sessionCookie;
    }

}
