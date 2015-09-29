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
