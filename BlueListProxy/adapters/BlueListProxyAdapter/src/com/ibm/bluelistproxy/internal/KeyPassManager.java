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
import java.lang.reflect.Method;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.worklight.adapters.rest.api.ConfigurationAPI;
import com.worklight.adapters.rest.api.WLServerAPI;
import com.worklight.core.oauth.api.OAuthUserIdentity;

/**
 * Handles functions related to credentials and encryption/decryption.
 */
public class KeyPassManager {
	private static final String CLASS_NAME = KeyPassManager.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);

    private WLServerAPI api = null;
    private static KeyPassManager instance = null;

    private Map<String,String> adminCredentials = null;
	private Map<String,Map<String,String>> userCredentialsCache = new HashMap<String,Map<String,String>>();
	
	private CloudantClient adminCloudant = null;
	private Map<String,CloudantClient> userCloudantCache = new HashMap<String,CloudantClient>();
	
    
	/**
	 * Default constructor.  This constructor is private and should only be used by the
	 * getInstance(api) method.  All users of this class should use the getInstance() method to 
	 * access an instance of the class.
	 * 
	 * During construction, either the VCAP configuration (off-premise) or the JNDI configuration (on-premise)
	 * will be read and stored.
	 */
    private KeyPassManager(WLServerAPI api) {
		final String METHOD_NAME = "KeyPassManager";
		logger.entering(CLASS_NAME, METHOD_NAME, api);
		
		this.api = api;
        
        // If vcap services defined, then create config based on them
        createVCAPConfiguration();
        
        // If vcap services not defined, then create config based on JNDI vars
        if (this.adminCredentials == null) {
        	createJNDIConfiguration(api.getConfigurationAPI());
        }
        
        // If JNDI vars not defined, then create config based on mobile first properties
        if (this.adminCredentials == null) {
        	createMFPropsConfiguration(api.getConfigurationAPI());
        }

		logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Obtains a singleton instance of this class.  Only the adapter startup 
     * should invoke this method.
     * 
     * @return The KeyPassManager singleton instance.
     */
    public static KeyPassManager getInstance(WLServerAPI api) {
		final String METHOD_NAME = "getInstance";
		logger.entering(CLASS_NAME, METHOD_NAME, api);
    	
    	// If first time, create an instance of the class (creating the configuration as well).
    	if (instance == null) {
    		instance = new KeyPassManager(api);
    	}
    	
    	// Return the singleton instance
		logger.exiting(CLASS_NAME, METHOD_NAME, instance);
    	return instance;
    }
    
    /**
     * Obtains a singleton instance of this class.  All users of the class 
     * should use the method when accessing the class.
     * 
     * @return The KeyPassManager singleton instance.
     */
    public static KeyPassManager getInstance() {
    	
    	// Return the singleton instance
    	return instance;
    }

    
    /**
     * Returns the cloudant admin credentials.
     * 
     * @return The admin credentials.
     * @throws BlueListProxyException If the admin credentials could not be obtained from the configuration.
     */
    public Map<String,String> getAdminCredentials()
    throws BlueListProxyException {
		final String METHOD_NAME = "getAdminCredentials";
		logger.entering(CLASS_NAME, METHOD_NAME);
		
    	if (this.adminCredentials == null) {
    		logger.severe("Unable to obtain admin credentials from VCAP_SERVICES environment variable (off-premise) or JNDI variables (on-premise)");
    		throw new BlueListProxyException(503, "Unable to obtain the admin credentials from the VCAP_SERVICES environment variable (off-premise) or JNDI variables (on-premise).");
    	}
    	
		logger.exiting(CLASS_NAME, METHOD_NAME);  // Don't log credentials
    	return this.adminCredentials;
    }
    
    /**
     * Returns the cloudant client based upon the admin credentials.
     * 
     * @return The cloudant client
     * @throws BlueListProxyException Thrown if invalid admin credentials.
     */
    public CloudantClient getAdminCloudantClient()
    throws BlueListProxyException {
		final String METHOD_NAME = "getAdminCloudantClient";
		logger.entering(CLASS_NAME, METHOD_NAME);
		
    	if (this.adminCloudant == null) {
    		try {
        		Map<String,String> adminCreds = getAdminCredentials();
        		String urlString = adminCreds.get("protocol") + "://" + adminCreds.get("host") + ":" + adminCreds.get("port");
        		this.adminCloudant = new CloudantClient(urlString, adminCreds.get("username"), adminCreds.get("password"));
    		}
    		catch(BlueListProxyException blpe) {
    			throw blpe;
    		}
    		catch(Exception e) {
        		logger.severe("Unable to obtain create cloudant client from admin credentials; error = " + e.getMessage());
        		throw new BlueListProxyException(503, "Unable to obtain create cloudant client from admin credentials.", e);
    		}
    	}
    	
		logger.exiting(CLASS_NAME, METHOD_NAME, this.adminCloudant);
    	return this.adminCloudant;
    }
    
    /**
     * Obtain the user credentials associated with the request.
     * 
     * @param request The servlet request.
     * @return The user credentials.
     * @throws BlueListProxyException Thrown if the user credentials could not be obtained from cloudant.
     */
    public Map<String,String> getUserCredentials(String userId)
    throws BlueListProxyException {
		final String METHOD_NAME = "getUserCredentials";
		logger.entering(CLASS_NAME, METHOD_NAME, userId);
		
		// Initialize returned user credentials
		Map<String,String> userCredentials = null;
		
		// If userId is null, throw exception
		if (userId == null) {
			logger.severe("Unable to obtain userId from security context or incoming request");
			throw new BlueListProxyException(401, "Unable to obtain userId from the security context or the incoming request.");
		}
		
		// If userId is empty, then return admin credentials with no username/password
		// in case authentication is not actually required.
		else if (userId.length() == 0) {
			userCredentials = new HashMap<String,String>(3);
			userCredentials.put("protocol", this.adminCredentials.get("protocol"));
			userCredentials.put("host", this.adminCredentials.get("host"));
			userCredentials.put("port", this.adminCredentials.get("port"));
			logger.fine("User ("+userId+") is empty; set credentials without user/password in case it is not required");
		}
		
		// Valid userId; obtain credentials from cache or cloudant
		else {
			
			// Get credentials from the cache
			userCredentials = this.userCredentialsCache.get(userId);
			
			// If credentials not found in cache, get them from cloudant
			if (userCredentials == null) {
				
				// Get user credentials from cloudant
				userCredentials = getUserCredentialsFromCloudant(userId);
				
				// If credentials found, cache them
				this.userCredentialsCache.put(userId, userCredentials);
				
			}
			
		}
    	
		logger.exiting(CLASS_NAME, METHOD_NAME);  // Don't log credentials
    	return userCredentials;
    }
    
    /**
     * Returns the cloudant client based upon the user credentials.
     * 
     * @param userId The user id.
     * @return The cloudant client
     * @throws BlueListProxyException Thrown if invalid user credentials.
     */
    public CloudantClient getUserCloudantClient(String userId)
    throws BlueListProxyException {
		final String METHOD_NAME = "getUserCloudantClient";
		logger.entering(CLASS_NAME, METHOD_NAME, userId);
		
		CloudantClient userCloudant = this.userCloudantCache.get(userId);
    	if (userCloudant == null) {
    		try {
    			Map<String,String> userCreds = this.userCredentialsCache.get(userId);
        		userCloudant = new CloudantClient(userCreds.get("protocol") + "://" + userCreds.get("host") + ":" + userCreds.get("port"), userCreds.get("username"), userCreds.get("password"));
        		this.userCloudantCache.put(userCreds.get("username"), userCloudant);
    		}
    		catch(Exception e) {
        		logger.severe("Unable to obtain create cloudant client from user credentials; error = " + e.getMessage());
        		throw new BlueListProxyException(503, "Unable to obtain create cloudant client from user credentials.", e);
    		}
    	}
    	
		logger.exiting(CLASS_NAME, METHOD_NAME, userCloudant);
    	return userCloudant;
    }
    
    /**
     * Removes cached information about a user.
     * 
     * @param userId The userId to clear.
     */
    public void clearCredentials(String userId) {
		final String METHOD_NAME = "clearCredentials";
		logger.entering(CLASS_NAME, METHOD_NAME, userId);
		
		// If valid userId provided, remove its cache entry
		if (userId != null) {
			this.userCredentialsCache.remove(userId);
			this.userCloudantCache.remove(userId);
			logger.fine("User ("+userId+") credentials cleared from cache");
		}
		
		// Invalid userId provided
		else {
			logger.fine("Unable to clear user credentials; no userId provided");
		}
    	
		logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Obtains user id from the security context.
     * If oauth being bypassed, get userId from payload.
     * 
     * @param request The servlet request.
     * @return The userId.
     */
    public String getUserId(HttpServletRequest request) {
		final String METHOD_NAME = "getUserId";
		logger.entering(CLASS_NAME, METHOD_NAME, request);
    	
		// Get oauth context and pull user id
		String userId = null;
    	OAuthUserIdentity userIdentity = api.getSecurityAPI().getSecurityContext().getUserIdentity();
    	if (userIdentity != null) {
        	logger.fine("id = " + userIdentity.getId() + "; realm = " + userIdentity.getRealm() + "; displayName = " + userIdentity.getDisplayName());
        	userId = userIdentity.getId();
    	}
    	else {
    		logger.fine("no user identity in security context");
    	}
    	
    	// If oauth context contains userId, we're done
    	if (userId != null) {
			logger.fine("Obtained user ("+userId+") from security context.");
		}
    	
    	// If oauth context does not have userId, try payload
    	else {
    		if (request.getContentLength() > 0) {
        		try {
        			JSONObject jsonObject = JSONObject.parse(request.getInputStream());
        			Object usernameObj = jsonObject.get("username");
        			userId = (usernameObj instanceof String ? (String)usernameObj : null);
        			logger.fine("Obtained user ("+userId+") from request payload.");
        		} catch(IOException ioe) {
        			logger.fine("Unable to parse payload to obtain username.");
        		}
    		}
    		
    		// If we still do not have a valid userId, then default to sample userId of 'james'
    		if (userId == null) {
    			userId = "james";
    			logger.fine("User not found in security context or request payload; defaulting to james.");
    		}
    		
    	}
    	
    	logger.exiting(CLASS_NAME, METHOD_NAME, userId);
    	return userId;
    }

	
	/**
	 * Obtain cloudant access configuration based up the VCAP environment variables
	 * (off-premise BlueMix environment).
	 * 
	 */
	private void createVCAPConfiguration() {
		final String METHOD_NAME = "createVCAPConfiguration";
		logger.entering(CLASS_NAME, METHOD_NAME);
    	
    	// Obtain VCAP_SERVICES environment variable
        String vcapServices = System.getenv("VCAP_SERVICES");
        logger.finest("VCAP_SERVICES = " + vcapServices);
		
		if (vcapServices != null) {
			
			// Remove any leading or trailing quotes
            boolean altered = false;
            if(vcapServices.startsWith("\"")  ||  vcapServices.startsWith("'")) {
                vcapServices = vcapServices.substring(1);
                altered = true;
            }
            if (vcapServices.endsWith("\"")  ||  vcapServices.endsWith("'")){
                vcapServices = vcapServices.substring(0, vcapServices.length()-1);
                altered = true;
            }
            if (altered){
                logger.finest("Cleaned VCAP_SERVICES = " + vcapServices);
        	}
            
            // Obtain JSON object from string
            JSONObject vcapServicesJson = null;
            try {
            	vcapServicesJson = JSONObject.parse(vcapServices);
            } catch(IOException ioe) {
            	logger.severe("Exception caught converting VCAP_SERVICES string to JSON; exception = " + ioe.getMessage());
            }
            
            // If JSON object is valid
            if (vcapServicesJson != null) {
            	
            	// Search for field starting with 'cloudantNoSQLDB' (such as 'cloudantNoSQLDB or 'cloudantNoSQLDB_Dev').
            	String cloudantNoSQLDBKey = null;
            	for (Object keyObj : vcapServicesJson.keySet() ) {
            		if (keyObj instanceof String  &&  ((String)keyObj).startsWith("cloudantNoSQLDB")) {
            			cloudantNoSQLDBKey = (String)keyObj;
            			break;
            		}
            	}

            	// Go through JSON object and pull out host, port, username, and password used to
            	// access cloudant as an admin.
	            if (cloudantNoSQLDBKey != null) {
	                Object cloudantVCAPObject = vcapServicesJson.get(cloudantNoSQLDBKey);
	                if (cloudantVCAPObject != null && cloudantVCAPObject instanceof JSONArray) {
	                    JSONArray cloudantVCAPJsonArray = (JSONArray)cloudantVCAPObject;
	                    if(cloudantVCAPJsonArray.size() > 0) {
	                        if(cloudantVCAPJsonArray.size() > 1) {
	                            logger.fine("Found more than one entry for cloudantNoSQLDB" +
	                                    " in the VCAP_SERVICES environment variable." + 
	                                    " Will attempt to use first entry, but best to ensure only one entry.");
	                    	}
	                        Object cloudantVCAPEntryObject = cloudantVCAPJsonArray.get(0);
	                        if (cloudantVCAPEntryObject != null  && cloudantVCAPEntryObject instanceof JSONObject) {
	                            JSONObject cloudantVCAPEntryJson = (JSONObject)cloudantVCAPEntryObject;
	                            Object cloudantCredentialsObject = cloudantVCAPEntryJson.get("credentials");
	                            if (cloudantCredentialsObject != null && cloudantCredentialsObject instanceof JSONObject) {
	                                JSONObject cloudantCredentialsJson = (JSONObject)cloudantCredentialsObject;

	                                // Pull cloudant access info: host, port, username, and password
	                                Object cloudantHostObject = cloudantCredentialsJson.get("host");
	                                Object cloudantPortObject = cloudantCredentialsJson.get("port");
	                                Object cloudantUsernameObject = cloudantCredentialsJson.get("username");
	                                Object cloudantPasswordObject = cloudantCredentialsJson.get("password");
	                        		
	                                logger.finest("host= " + cloudantHostObject);
	                                logger.finest("port= " + cloudantPortObject);
	                                logger.finest("username= " + cloudantUsernameObject);
	                                logger.finest("password= " + (cloudantPasswordObject != null ? "********" : "null"));
		                    		
		                    		// Convert JSON objects to strings
	                                String cloudantProtocol = "https";
	                                String cloudantHost = (cloudantHostObject instanceof String ? (String)cloudantHostObject : null);
	                                String cloudantPort = (cloudantPortObject instanceof Long ? ((Long)cloudantPortObject).toString() : null);
	                                String cloudantUsername = (cloudantUsernameObject instanceof String ? (String)cloudantUsernameObject : null);
	                                String cloudantPassword = (cloudantPasswordObject instanceof String ? (String)cloudantPasswordObject : null);
	                        		
	                                // If all fields are present, then update the corresponding config vars
	                        		createAdminCredentials(cloudantProtocol, cloudantHost, cloudantPort, cloudantUsername, cloudantPassword);
	                                
	                            }
	                            else {
	                            	logger.fine("Invalid cloudantCredentialsObject ["+(cloudantCredentialsObject != null ? cloudantCredentialsObject.getClass().getName() : "null")+"].");
	                        	}
	                        }
	                        else {
	                        	logger.fine("Invalid cloudantVCAPEntryObject ["+(cloudantVCAPEntryObject != null ? cloudantVCAPEntryObject.getClass().getName() : "null")+"].");
                        	}
	                    }
	                    else {
	                    	logger.fine("cloudantVCAPJsonArray object has no items.");
	                	}
	                }
	                else {
	                	logger.fine("Invalid cloudantVCAPObject ["+(cloudantVCAPObject != null ? cloudantVCAPObject.getClass().getName() : "null")+"].");
                	}
	            }
	            else {
	            	logger.fine("VCAP_SERVICES environment variable JSON does not contain a cloudantNoSQLDB field.");
	        	}
	            
            }
            else {
    			logger.fine("VCAP_SERVICES JSON does not exist; no more work to do");
        	}
			
		}

		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	/**
	 * Create and store a configuration based up the JNDI variables.
	 * 
	 */
	private void createJNDIConfiguration(ConfigurationAPI configAPI) {
		final String METHOD_NAME = "createJNDIConfiguration";
		logger.entering(CLASS_NAME, METHOD_NAME, configAPI);
    	
        if (configAPI != null) {

            String cloudantProtocol = configAPI.getServerJNDIProperty("CloudantProtocol");
            String cloudantHost = configAPI.getServerJNDIProperty("CloudantHost");
            String cloudantPort = configAPI.getServerJNDIProperty("CloudantPort"); 
            String cloudantUsername = configAPI.getServerJNDIProperty("CloudantUsername");
            String encodedCloudantPassword = configAPI.getServerJNDIProperty("CloudantPassword");
            String cloudantPassword = (encodedCloudantPassword != null ? decode(encodedCloudantPassword) : null);

            logger.finest("CloudantProtocol= " + cloudantProtocol);
            logger.finest("CloudantHost= " + cloudantHost);
            logger.finest("CloudantPort= " + cloudantPort);
            logger.finest("CloudantUsername= " + cloudantUsername);
            logger.finest("CloudantPassword= " + (encodedCloudantPassword != null ? "********" : "null"));
    		
            // If all fields are present, then update the corresponding config vars
    		createAdminCredentials(cloudantProtocol, cloudantHost, cloudantPort, cloudantUsername, cloudantPassword);
            
        } else {
            logger.fine("ConfigurationAPI is null. This means the adapter API is not properly functioning and the configuration API cannot be obtained.");
        }

		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	/**
	 * Create and store a configuration based up the MobileFirst properties.
	 * 
	 */
	private void createMFPropsConfiguration(ConfigurationAPI configAPI) {
		final String METHOD_NAME = "createMFPropsConfiguration";
		logger.entering(CLASS_NAME, METHOD_NAME, configAPI);
    	
        if (configAPI != null) {

        	String cloudantProtocol = configAPI.getMFPConfigurationProperty("CloudantProtocol");
        	String cloudantHost = configAPI.getMFPConfigurationProperty("CloudantHost");
        	String cloudantPort = configAPI.getMFPConfigurationProperty("CloudantPort"); 
        	String cloudantUsername = configAPI.getMFPConfigurationProperty("CloudantUsername");
        	String encodedCloudantPassword = configAPI.getMFPConfigurationProperty("CloudantPassword");
            String cloudantPassword = (encodedCloudantPassword != null ? decode(encodedCloudantPassword) : null);

            logger.finest("CloudantProtocol= " + cloudantProtocol);
            logger.finest("CloudantHost= " + cloudantHost);
            logger.finest("CloudantPort= " + cloudantPort);
            logger.finest("CloudantUsername= " + cloudantUsername);
            logger.finest("CloudantPassword= " + (encodedCloudantPassword != null ? "********" : "null"));
    		
            // If all fields are present, then update the corresponding config vars
    		createAdminCredentials(cloudantProtocol, cloudantHost, cloudantPort, cloudantUsername, cloudantPassword);
            
        } else {
            logger.fine("ConfigurationAPI is null. This means the adapter API is not properly functioning and the configuration API cannot be obtained.");
        }

		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	private void createAdminCredentials(String protocol, String host, String port, String username, String password) {
		final String METHOD_NAME = "createAdminCredentials";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {protocol, host, port, username, (password != null ? "********" : "null")});

		
        // If all fields are present, then update the corresponding config vars
		if (protocol != null  &&  host != null  &&  port != null  &&
			username != null  &&  password != null) {
			this.adminCredentials = new HashMap<String,String>(5);
			this.adminCredentials.put("protocol", protocol);
			this.adminCredentials.put("host", host);
			this.adminCredentials.put("port", port);
			this.adminCredentials.put("username", username);
			this.adminCredentials.put("password", password);
		}
		else {
			logger.fine("Configuration is incomplete and cannot be used");
    	}

		logger.exiting(CLASS_NAME, METHOD_NAME);
	}
	
	/**
	 * Obtain password for given userId from cloudant _users database.
	 * 
	 * @param userId The user id.
	 * @return The full set of credentials including the password.
	 * @throws BlueListProxyException If user information could not be retrieved.
	 */
	public Map<String,String> getUserCredentialsFromCloudant(String userId) 
	throws BlueListProxyException {
		final String METHOD_NAME = "getUserCredentialsFromCloudant";
		logger.entering(CLASS_NAME, METHOD_NAME, userId);
		
		Map<String,String> userCredentials = null;
		
		// Get user credentials
		InputStream jsonStream = null;
		try {
			
			logger.fine("Getting user credentials for: " + userId);
			Database db = getAdminCloudantClient().database("_users", false);
			jsonStream = db.find("org.couchdb.user:" + userId);
			JSONObject jsonObject = JSONObject.parse(jsonStream);
			logger.fine("Obtained user credentials for: " + userId + " successfully.");
			
			// Pull password and salt from result and decrypt it
			String encryptedPassword = (String)jsonObject.get("password");
			String salt = (String)jsonObject.get("salt");
			String password = decryptWithSalt(encryptedPassword, salt);
			
			// If all fields obtained successfully, then create the user credentials
			userCredentials = new HashMap<String,String>(5);
			userCredentials.put("protocol", this.adminCredentials.get("protocol"));
			userCredentials.put("host", this.adminCredentials.get("host"));
			userCredentials.put("port", this.adminCredentials.get("port"));
			userCredentials.put("username", userId);
			userCredentials.put("password", password);
			logger.fine("Obtained valid credentials for user ("+userId+").");
			
		} catch(BlueListProxyException blpe) {
	    	logger.severe("Failed to obtain valid credentials for user ("+userId+"); error = " + blpe.getMessage());
			throw blpe;
		} catch(Exception e) {
	    	logger.severe("Failed to obtain valid credentials for user ("+userId+"); error = " + e.getMessage());
	    	throw new BlueListProxyException("Failed to obtain valid credentials for user ("+userId+")", e);
		} finally {
			if (jsonStream != null) {
				try { 
					jsonStream.close(); 
				} catch(Exception e) {}
			}
		}

		logger.exiting(CLASS_NAME, METHOD_NAME);  // Do not log credentials
		return userCredentials;
	}

    /**
     * Encrypt the given password with the given salt value.
     * 
     * @param value The password.
     * @param salt The salt value.
     * @return The encrypted password.
     * @throws BlueListProxyException Thrown if the password could not be encrypted.
     */
	public byte[] encryptWithSalt(String value, String salt)
	throws BlueListProxyException {
		final String METHOD_NAME = "encryptWithSalt";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {(value != null ? "********" : "null"),(salt != null ? "********" : "null")});
		
		byte[] encryptedValue = null;
		
		try {
			
			byte[] keyb = salt.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");

			int keySize = getMaxEncryptionBlockSize();
			final byte[][] keyAndIV = EVP_BytesToKey(keySize, 16, md, keyb);
			SecretKeySpec key = new SecretKeySpec(keyAndIV[0],"AES");
			IvParameterSpec iv = new IvParameterSpec(keyAndIV[1]);
			
			byte[] decrypted = value.getBytes("UTF-8");
			encryptedValue = encrypt(key, iv, decrypted);
			
		} catch(Exception e) {
			logger.severe("Exception caught encrypting password with salt value; exception = " + e.getMessage());
			throw new BlueListProxyException("Exception caught encrypting password with salt value", e);
		}
	
		logger.exiting(CLASS_NAME, METHOD_NAME, (encryptedValue != null ? "********" : "null"));
		return encryptedValue;
	}

    /**
     * Decrypt the given password with the given salt.
     * 
     * @param value The encrypted password.
     * @param salt The salt value.
     * @return The decrypted password.
     * @throws BlueListProxyException Thrown if the password could not be decrypted.
     */
	private String decryptWithSalt(String value, String salt) 
	throws BlueListProxyException {
		final String METHOD_NAME = "decryptWithSalt";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {(value != null ? "********" : "null"),(salt != null ? "********" : "null")});
		
		String decryptedValue = null;
		
		try {

			// Convert String to byte array
			int len = value.length() / 2;
			byte[] valueBytes = new byte[len];
			for (int i = 0; i < len; i++) {
				valueBytes[i] = Integer.valueOf(value.substring(2 * i, 2 * i + 2),16).byteValue();
			}
			
			byte[] keyb = salt.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");

			int keySize = getMaxEncryptionBlockSize();
			final byte[][] keyAndIV = EVP_BytesToKey(keySize, 16, md, keyb);
			SecretKeySpec key = new SecretKeySpec(keyAndIV[0],"AES");
			IvParameterSpec iv = new IvParameterSpec(keyAndIV[1]);
			byte[] decryptedBytes = decrypt(key, iv, valueBytes);
			
			decryptedValue = new String (decryptedBytes, "UTF-8");
			
		} catch(Exception e) {
			logger.severe("Exception caught decrypting password with salt value; exception = " + e.getMessage());
			throw new BlueListProxyException("Exception caught decrypting password with salt value", e);
		}

		logger.exiting(CLASS_NAME, METHOD_NAME, (decryptedValue != null ? "********" : "null"));
		return decryptedValue;
	}

	/**
	 * 
	 * @param key_len
	 * @param iv_len
	 * @param md
	 * @param data
	 * @return
	 */
	private byte[][] EVP_BytesToKey(int key_len, int iv_len, MessageDigest md, byte[] data) {
		final String METHOD_NAME = "EVP_BytesToKey";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {key_len, iv_len, md, (data != null ? "********" : "null")});
		
		byte[][] both = new byte[2][];
		byte[] key = new byte[key_len];
		int key_ix = 0;
		byte[] iv = new byte[iv_len];
		int iv_ix = 0;
		both[0] = key;
		both[1] = iv;
		byte[] md_buf = null;
		int nkey = key_len;
		int niv = iv_len;
		int i = 0;

		if (data == null) {
			logger.exiting(CLASS_NAME, METHOD_NAME, (both != null ? "********" : "null"));
			return both;
		}

		int addmd = 0;
		for (;;) {
			md.reset();
			if (addmd++ > 0) {
				md.update(md_buf);
			}
			md.update(data);

			md_buf = md.digest();

			i = 0;
			if (nkey > 0) {
				for (;;) {
					if (nkey == 0)
						break;
					if (i == md_buf.length)
						break;
					key[key_ix++] = md_buf[i];
					nkey--;
					i++;
				}
			}
			if (niv > 0 && i != md_buf.length) {
				for (;;) {
					if (niv == 0)
						break;
					if (i == md_buf.length)
						break;
					iv[iv_ix++] = md_buf[i];
					niv--;
					i++;
				}
			}
			if (nkey == 0 && niv == 0) {
				break;
			}
		}
		for (i = 0; i < md_buf.length; i++) {
			md_buf[i] = 0;
		}

		logger.exiting(CLASS_NAME, METHOD_NAME, (both != null ? "********" : "null"));
		return both;
	}

	/**
	 * 
	 * @param key
	 * @param iv
	 * @param plain
	 * @return
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	private byte[] encrypt(SecretKeySpec key, IvParameterSpec iv, byte[] plain) 
	throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
		   NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		final String METHOD_NAME = "encrypt";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {key, iv, (plain != null ? "********" : "null")});
		
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key, iv);
		byte[] encrypted = cipher.doFinal(plain);

		logger.exiting(CLASS_NAME, METHOD_NAME, (encrypted != null ? "********" : "null"));
		return encrypted;
	}

	/**
	 * 
	 * @param key
	 * @param iv
	 * @param encrypted
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	private byte[] decrypt(SecretKeySpec key, IvParameterSpec iv, byte[] encrypted) 
	throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
		   InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		final String METHOD_NAME = "decrypt";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {key, iv, (encrypted != null ? "********" : "null")});
		
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, key, iv);
		byte[] clearbyte = cipher.doFinal(encrypted);

		logger.exiting(CLASS_NAME, METHOD_NAME, (clearbyte != null ? "********" : "null"));
		return clearbyte;
	}

	/**
	 * 
	 * @return
	 */
	private int getMaxEncryptionBlockSize(){
		final String METHOD_NAME = "getMaxEncryptionBlockSize";
		logger.entering(CLASS_NAME, METHOD_NAME);
		
		int maxBlockSize = 16;
		try{
			maxBlockSize = (Cipher.getMaxAllowedKeyLength("AES/CBC/PKCS5Padding") > 128 ? 32 : 16);
		}catch(NoSuchAlgorithmException e){
			maxBlockSize = 16;
		}

		logger.exiting(CLASS_NAME, METHOD_NAME, maxBlockSize);
		return maxBlockSize;
	}	
	
	/**
	 * Decode the password with the Liberty/WAS encoding for On-premise
	 * 
	 * @param pwd password
	 * @return the decoded password
	 */
    private String decode(String pwd) {
        final String METHOD_NAME = "decode";
        logger.entering(CLASS_NAME, METHOD_NAME, (pwd != null ? "********" : "null"));
		
        try {
            if (pwd != null) {
                // Is password encoded ?
                Class<?>[] paramString = new Class[1];  
                paramString[0] = String.class;
                Class<?> cls = Class.forName("com.ibm.websphere.crypto.PasswordUtil");
                Object pwdutil = cls.newInstance();
                Method methodClass = cls.getMethod("isEncrypted", paramString);
                Boolean encoded = (Boolean) methodClass.invoke(pwdutil, pwd);
                if (encoded) {
                    // Decode the password
                    methodClass = cls.getMethod("decode", paramString);
                    pwd = (String) methodClass.invoke(pwdutil, pwd);
                }
            }
        } catch (Exception e) {
            // The PasswordUtil jar is missing, we keep the password
            logger.warning("Impossible to decode the password");
        }

        logger.exiting(CLASS_NAME, METHOD_NAME, (pwd != null ? "********" : "null"));
        return pwd;
    }

}
