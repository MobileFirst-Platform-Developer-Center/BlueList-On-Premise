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
