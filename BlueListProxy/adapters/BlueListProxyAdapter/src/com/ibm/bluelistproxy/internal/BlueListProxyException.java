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

/**
 * BlueList proxy exception.
 * Wraps a general exception with an additional http status code.
 */
public class BlueListProxyException extends Exception {
	private static final long serialVersionUID = 1L;
	private static final String CLASS_NAME = BlueListProxyException.class.getName();
	private static final Logger logger = Logger.getLogger(CLASS_NAME);
	
	private int statusCode;

	/**
	 * Default constructor. Default to 500 status code.
	 */
	public BlueListProxyException() {
		this(500);
	}

	/**
	 * Default constructor. Default to 500 status code.
	 * 
	 * @param message Exception message.
	 */
	public BlueListProxyException(String message) {
		this(500, message);
	}

	/**
	 * Default constructor. Default to 500 status code.
	 * 
	 * @param cause Nested exception.
	 */
	public BlueListProxyException(Throwable cause) {
		this(500, cause);
	}

	/**
	 * Default constructor. Default to 500 status code.
	 * 
	 * @param message Exception message.
	 * @param cause Nested exception.
	 */
	public BlueListProxyException(String message, Throwable cause) {
		this(500, message, cause);
	}

	/**
	 * Exception constructor with status code.
	 * 
	 * @param statusCode The http status code.
	 */
	public BlueListProxyException(int statusCode) {
		super();
		
		final String METHOD_NAME = "BlueListProxyException";
		logger.entering(CLASS_NAME, METHOD_NAME, statusCode);
		
		this.statusCode = statusCode;
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}

	/**
	 * Exception constructor with status code.
	 * 
	 * @param statusCode The http status code.
	 * @param message Exception message.
	 */
	public BlueListProxyException(int statusCode, String message) {
		super(message);
		
		final String METHOD_NAME = "BlueListProxyException";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {statusCode, message});
		
		this.statusCode = statusCode;
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}

	/**
	 * Exception constructor with status code.
	 * 
	 * @param statusCode The http status code.
	 * @param cause Nested exception.
	 */
	public BlueListProxyException(int statusCode, Throwable cause) {
		super(cause);
		
		final String METHOD_NAME = "BlueListProxyException";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {statusCode, cause});
		
		this.statusCode = statusCode;
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}

	/**
	 * Exception constructor with status code.
	 * 
	 * @param statusCode The http status code.
	 * @param message Exception message.
	 * @param cause Nested exception.
	 */
	public BlueListProxyException(int statusCode, String message, Throwable cause) {
		super(message, cause);
		
		final String METHOD_NAME = "BlueListProxyException";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {statusCode, message, cause});
		
		this.statusCode = statusCode;
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	/**
	 * Getter for http status code.
	 * 
	 * @return Http status code.
	 */
	public int getStatusCode() {
		return this.statusCode;
	}
	
	/**
	 * Setter for http status code.
	 * 
	 * @param statusCode Http status code.
	 */
	public void setStatusCode(int statusCode) {
		final String METHOD_NAME = "setStatusCode";
		logger.entering(CLASS_NAME, METHOD_NAME, statusCode);
		
		this.statusCode = statusCode;
		
		logger.exiting(CLASS_NAME, METHOD_NAME);
	}

}
