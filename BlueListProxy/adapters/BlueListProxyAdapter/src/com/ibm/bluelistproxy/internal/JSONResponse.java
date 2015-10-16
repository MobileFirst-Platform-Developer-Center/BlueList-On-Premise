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
