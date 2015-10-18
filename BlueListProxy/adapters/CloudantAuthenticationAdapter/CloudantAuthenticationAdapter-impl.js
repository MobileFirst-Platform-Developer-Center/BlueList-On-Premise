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

function onAuthRequired(headers, errorMessage){
	WL.Logger.warn("CloudantAuthenticationAdapter: onAuthRequired(): entry/exit.");
	errorMessage = errorMessage ? errorMessage : null;

	return {
		authRequired: true,
		errorMessage: errorMessage
	};
}

function submitAuthentication(username, password){
	WL.Logger.warn("CloudantAuthenticationAdapter: submitAuthentication(): entry: username = " + username + "; password = " + password + ".");
	if (username==="james" && password === "42"){

		var userIdentity = {
				userId: username,
				displayName: username,
				attributes: {
					age: "42"
				}
		};

		WL.Server.setActiveUser("cloudant", userIdentity);

		WL.Logger.warn("CloudantAuthenticationAdapter: submitAuthentication(): exit: userIdentity = " + userIdentity + ".");
		return {
			authRequired: false
		};
	}

	return onAuthRequired(null, "Invalid login credentials");
}

function getSecretData(){
	WL.Logger.warn("CloudantAuthenticationAdapter: getSecretData(): entry/exit.");
	return {
		secretData: "r79puu!0@e5bQ!MosbN45KKgnY#CyRp*cVBGDwNJ23BjoOQQ"
	};
}

function onLogout(){
	WL.Logger.warn("CloudantAuthenticationAdapter: onLogout(): entry/exit.");
	WL.Logger.warn("Logged out");
}
