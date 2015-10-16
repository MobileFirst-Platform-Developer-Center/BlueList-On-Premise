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
package com.ibm.bluelistproxy.internal.crypto;

import java.security.Key;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import com.ibm.bluelistproxy.internal.BlueListProxyException;
import com.ibm.bluelistproxy.internal.KeyPassManager;


/**
 * Provides simple encrypt and decrypt utilities using AES and an internal 16 byte key.
 * The given text and salt value are encrypted and stored.  The text can be pulled from the
 * decrypted value by stripping of the salt value.
 */
public class BlueListCryptoBasic implements BlueListCrypto {
	private static final String CLASS_NAME = KeyPassManager.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);
    
    private static final String ALGORITHM = "AES";
    private static final byte[] KEY_BYTES = new byte[] { 
    	(byte)0x47, (byte)0xf3, (byte)0xa6, (byte)0x27, 
        (byte)0xbc, (byte)0x94, (byte)0x94, (byte)0xe5, 
        (byte)0x58, (byte)0xa0, (byte)0x1c, (byte)0xdf, 
        (byte)0x01, (byte)0x43, (byte)0x90, (byte)0x0f };  // Must be 16 bytes
	
    /**
     * Base constructor.
     */
	public BlueListCryptoBasic() {
	}

	@Override
	public String encrypt(String text, String salt)
	throws BlueListProxyException {
		final String METHOD_NAME = "encrypt";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {(text != null ? "********" : "null"),(salt != null ? "********" : "null")});
		
		String encString = null;
		
		try {
			
			// The decrypted string is the given string plus the salt string
			String decText = text + salt;
			byte[] decBytes = decText.getBytes("UTF-8");

			// Encrypt using the encrypt cipher
			Cipher cipher = Cipher.getInstance( ALGORITHM );
			Key key = new SecretKeySpec( KEY_BYTES, ALGORITHM );
			cipher.init( Cipher.ENCRYPT_MODE, key );
			byte[] encBytes = cipher.doFinal(decBytes);
			
			// Convert bytes to hex string
		    encString = Hex.encodeHexString(encBytes);
		    
		} catch(Exception e) {
			logger.severe("Exception caught encrypting password with salt value; exception = " + e.getMessage());
			throw new BlueListProxyException("Exception caught encrypting password with salt value", e);
		}
		
		logger.exiting(CLASS_NAME, METHOD_NAME, encString);
		return encString;
	}

	@Override
	public String decrypt(String text, String salt)
	throws BlueListProxyException {
		final String METHOD_NAME = "decrypt";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {(text != null ? "********" : "null"),(salt != null ? "********" : "null")});
		
		String decString = null;
		
		try {

			// Convert String to byte array
			int len = text.length() / 2;
			byte[] encBytes = new byte[len];
			for (int i = 0; i < len; i++) {
				encBytes[i] = Integer.valueOf(text.substring(2 * i, 2 * i + 2),16).byteValue();
			}

			// Decrypt using the decrypt cipher
			Cipher cipher = Cipher.getInstance( ALGORITHM );
			Key key = new SecretKeySpec( KEY_BYTES, ALGORITHM );
			cipher.init( Cipher.DECRYPT_MODE, key );
			byte[] decBytes = cipher.doFinal(encBytes);
			
			// Pull password from string; everything up to salt value
			String passwordsalt = new String(decBytes, "UTF-8");
			int saltIndex = passwordsalt.indexOf(salt);
			if ( saltIndex >= 0 ) decString = passwordsalt.substring(0, saltIndex);
		    
		} catch(Exception e) {
			logger.severe("Exception caught decrypting password with salt value; exception = " + e.getMessage());
			throw new BlueListProxyException("Exception caught decrypting password with salt value", e);
		}
		
		logger.exiting(CLASS_NAME, METHOD_NAME, decString);
		return decString;
	}

}
