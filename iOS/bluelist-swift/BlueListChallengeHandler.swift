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


import Foundation



class BlueListChallengeHandler: ChallengeHandler {
    
    override init() {
    super.init(realm: "CLOUDANT_SCOPE")
    
    }

    override func isCustomResponse(response: WLResponse!) -> Bool {
     
        if (response != nil && response.getResponseJson() != nil) {
            let responseJSON = response.getResponseJson() as NSDictionary
            let authRequired = responseJSON.objectForKey("authRequired") as! Bool?
            if authRequired != nil {
                return authRequired!
            }
        }
        return false
    }

    override func handleChallenge(response:WLResponse!){
        NSLog("Inside handleChallenge - silently loggin in")
        let invocationData = WLProcedureInvocationData(adapterName: "CloudantAuthenticationAdapter", procedureName: "submitAuthentication")
        invocationData.parameters = ["james","42"]
        submitAdapterAuthentication(invocationData, options: nil)

    }

    override func onSuccess(response: WLResponse!)
    {
        submitSuccess(response)
    
    }
    override func onFailure(response:WLResponse!)
    {
        submitFailure(response)
        
    }
}