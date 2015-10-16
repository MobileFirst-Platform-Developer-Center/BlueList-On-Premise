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
package com.ibm.bluelistproxy;

import java.util.logging.Logger;

import com.worklight.wink.extensions.MFPJAXRSApplication;

/**
 * Base JAX-RS application class.
 */
public class BlueListProxyAdapterApplication extends MFPJAXRSApplication{
	private static final String CLASS_NAME = BlueListProxyAdapterApplication.class.getName();
	private static final Logger logger = Logger.getLogger(CLASS_NAME);
	
	@Override
	protected void init() throws Exception {
    	final String METHOD_NAME = "init";
    	logger.entering(CLASS_NAME, METHOD_NAME);
    	
		super.init();
		logger.info("Adapter initialized!");

    	logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	@Override
	protected void destroy() throws Exception {
    	final String METHOD_NAME = "destroy";
    	logger.entering(CLASS_NAME, METHOD_NAME);
    	
		super.destroy();
		logger.info("Adapter destroyed!");
		
    	logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	@Override
	protected String getPackageToScan() {
		//The package of this class will be scanned (recursively) to find JAX-RS resources. 
		//It is also possible to override "getPackagesToScan" method in order to return more than one package for scanning
		return getClass().getPackage().getName();
	}
}
