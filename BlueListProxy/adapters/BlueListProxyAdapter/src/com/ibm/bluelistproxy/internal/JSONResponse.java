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

import com.ibm.json.java.JSONObject;

/**
 * JSON response corresponding to response received from cloudant.
 */
public class JSONResponse {
	private static final String CLASS_NAME = JSONResponse.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);

	private int statusCode = 0;
	private Map<String,String> headers = null;
	private String contentType = null;
	private JSONObject result = null;

	/**
	 * Constructor with response http status code, headers, and the payload.
	 * 
	 * @param statusCode The http status code.
	 * @param headers The headers.
	 * @param result The payload (JSON object).
	 */
	public JSONResponse(int statusCode, Map<String,String> headers, JSONObject result) {
		final String METHOD_NAME = "JSONResponse";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {statusCode, headers, result});
		
		this.statusCode = statusCode;
		this.headers = headers;
		this.contentType = "application/json";
		this.result = result;

		logger.exiting(CLASS_NAME, METHOD_NAME);
	}

	/**
	 * @return the statusCode
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * @return the headers
	 */
	public Map<String,String> getHeaders() {
		return headers;
	}

	/**
	 * @return the contentType
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * @return the result
	 */
	public JSONObject getResult() {
		return result;
	}

	/**
	 * @return the result as a string
	 */
	public String getResultAsString() {
		return (this.result != null ? this.result.toString() : "");
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "JSONResponse [result=" + getResultAsString() + ", contentType = " + getContentType()
				+ ", statusCode=" + getStatusCode() + ", headers = " + getHeaders() + "]";
	}

}
