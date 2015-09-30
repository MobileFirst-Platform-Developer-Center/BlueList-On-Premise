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

import com.ibm.bluelistproxy.internal.BlueListProxyException;

/**
 * Interface for objects that provide encryption and decryption functions.
 * 
 * There are two implementations provided:
 *  - BlueListCryptoBasic - provides basic encryption/decryption using AES.  It is a 
 *    relatively simple implementation, but it is not compatible with the Bluemix BlueList
 *    nodejs sample proxy.
 *  - BlueListCryptoNodejs - provides a much more complex encryption/decryption implementation 
 *    that is compatible with the Bluemix BlueList nodejs sample proxy.
 *    
 * By default the BlueListCryptoBasic implementation is used.  If it is desired to use the 
 * BlueListCryptoNodejs implementation, the USE_NODEJS_CRYPTO value should be set to true.  This
 * property is set in the same place that you set the Cloudant properties (a MobileFirst property, 
 * a JNDI property, or an ENV variable).
 */
public interface BlueListCrypto {
	
	/**
	 * Encrypt the given text using the given salt value.
	 * 
	 * @param text The text to encrypt.
	 * @param salt The salt value.
	 * @return The encrypted text.
	 * @throws BlueListProxyException Thrown if an error occurred during encryption.
	 */
	public String encrypt(String text, String salt)
	throws BlueListProxyException;

	/**
	 * Decrypt the given text using the given salt value.
	 * 
	 * @param text The text to decrypt.
	 * @param salt The salt value.
	 * @return The decrypted text.
	 * @throws BlueListProxyException Thrown if an error occurred during decryption.
	 */
	public String decrypt(String text, String salt)
	throws BlueListProxyException;

}
