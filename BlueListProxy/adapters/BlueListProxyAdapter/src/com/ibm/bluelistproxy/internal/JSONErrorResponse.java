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

import java.util.logging.Logger;

import com.ibm.json.java.JSONObject;

/**
 * JSON response for errors/exceptions.
 * Creates a payload with a status and message.
 */
public class JSONErrorResponse extends JSONResponse {
	private static final String CLASS_NAME = JSONErrorResponse.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);

	/**
	 * Constructor with an exception.
	 * 
	 * @param blpe The exception.
	 */
	public JSONErrorResponse(BlueListProxyException blpe) {
		super(blpe.getStatusCode(), null, getResult(blpe));
		
		final String METHOD_NAME = "JSONErrorResponse";
		logger.entering(CLASS_NAME, METHOD_NAME, blpe);
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}

	/**
	 * Constructor with status code and an exception.
	 * 
	 * @param statusCode The http status code.
	 * @param blpe The exception.
	 */
	public JSONErrorResponse(int statusCode, BlueListProxyException blpe) {
		super(statusCode, null, getResult(blpe));
		
		final String METHOD_NAME = "JSONErrorResponse";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {statusCode, blpe});
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	/**
	 * Creates a JSON object to be returned in the http response based
	 * on the given exception.
	 * 
	 * @param blpe The exception.
	 * @return The JSON object.
	 */
	private static JSONObject getResult(BlueListProxyException blpe) {
		final String METHOD_NAME = "getResult";
		logger.entering(CLASS_NAME, METHOD_NAME, blpe);
		
		JSONObject rootObj = new JSONObject();
		rootObj.put("status", "error");
		rootObj.put("message", blpe.getMessage());
		
		logger.exiting(CLASS_NAME, METHOD_NAME, rootObj);
		return rootObj;
	}

}
