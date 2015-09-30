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
package com.ibm.bluelistproxy.internal.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import com.ibm.bluelistproxy.internal.BlueListProxyException;
import com.ibm.bluelistproxy.internal.KeyPassManager;

/**
 * Provides an implementation of BlueListEncrypt which encrypts and decrypts in the 
 * same manner as the node.js crypto package.  It is fairly complex, but allows this 
 * sample to run against a cloudant database in conjunction with the Bluemix BlueList nodejs 
 * sample.
 */
public class BlueListCryptoNodejs implements BlueListCrypto {
	private static final String CLASS_NAME = KeyPassManager.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);
    
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
	
    /**
     * Base constructor.
     */
    public BlueListCryptoNodejs() {
	}
	
	@Override
	public String encrypt(String text, String salt)
	throws BlueListProxyException {
		final String METHOD_NAME = "encrypt";
		logger.entering(CLASS_NAME, METHOD_NAME, new Object[] {(text != null ? "********" : "null"),(salt != null ? "********" : "null")});
		
		String encString = null;
		
		try {
			
			// The decrypted string is the given string plus the salt string
			String decText = text;
			byte[] decBytes = decText.getBytes("UTF-8");

			// Encrypt using the encrypt cipher
			byte[] keyb = salt.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");
			int keySize = getMaxEncryptionBlockSize();
			final byte[][] keyAndIV = EVP_BytesToKey(keySize, 16, md, keyb);
			SecretKeySpec key = new SecretKeySpec(keyAndIV[0],"AES");
			IvParameterSpec iv = new IvParameterSpec(keyAndIV[1]);
			Cipher cipher = Cipher.getInstance( ALGORITHM );
			cipher.init(Cipher.ENCRYPT_MODE, key, iv);
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
			byte[] keyb = salt.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");
			int keySize = getMaxEncryptionBlockSize();
			final byte[][] keyAndIV = EVP_BytesToKey(keySize, 16, md, keyb);
			SecretKeySpec key = new SecretKeySpec(keyAndIV[0],"AES");
			IvParameterSpec iv = new IvParameterSpec(keyAndIV[1]);
			Cipher cipher = Cipher.getInstance( ALGORITHM );
			cipher.init(Cipher.DECRYPT_MODE, key, iv);
			byte[] decBytes = cipher.doFinal(encBytes);
			
			// Pull password from string; everything up to salt value
			decString = new String(decBytes, "UTF-8");
		    
		} catch(Exception e) {
			logger.severe("Exception caught decrypting password with salt value; exception = " + e.getMessage());
			throw new BlueListProxyException("Exception caught decrypting password with salt value", e);
		}
		
		logger.exiting(CLASS_NAME, METHOD_NAME, decString);
		return decString;
	}
	
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

}
