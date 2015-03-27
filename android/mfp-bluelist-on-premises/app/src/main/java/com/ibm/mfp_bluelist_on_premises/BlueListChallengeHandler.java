/**
 * Copyright 2015 IBM Corp. All Rights Reserved.
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
package com.ibm.mfp_bluelist_on_premises;

import android.util.Log;

import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLProcedureInvocationData;
import com.worklight.wlclient.api.WLRequestOptions;
import com.worklight.wlclient.api.WLResponse;
import com.worklight.wlclient.api.challengehandler.ChallengeHandler;

import org.json.JSONException;

/**
 *
 */
public class BlueListChallengeHandler extends ChallengeHandler {
    public String UserName;
    public String UserPassword;
    public String AdapterName;
    public String ScopeRealm;

    public BlueListChallengeHandler(String realm) {
        super(realm);
    }

    /**
     * Checks every custom response received from MobileFirst Server to see if that’s the challenge we are expecting.
     * @param response The MobileFirst response.
     * @return True if response contains an authRequired variable.
     */
    @Override
    public boolean isCustomResponse(WLResponse response) {
        try {
            if(response!= null &&
                    response.getResponseJSON()!=null &&
                    !response.getResponseJSON().isNull("authRequired") &&
                    response.getResponseJSON().getBoolean("authRequired")){
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("CHALLENGE_HANDLER", e.getMessage());

        }
        return false;
    }

    /**
     * Used to log in after the appropriate response from the MobileFirst server.
     * @param wlResponse The MobileFirst response.
     */
    @Override
    public void handleChallenge(WLResponse wlResponse) {
        Object[] parameters = new Object[]{UserName,UserPassword};
        WLProcedureInvocationData invocationData = new  WLProcedureInvocationData(AdapterName, "submitAuthentication");
        invocationData.setParameters(parameters);
        WLRequestOptions options = new WLRequestOptions();
        options.setTimeout(30000);
        submitAdapterAuthentication(invocationData, options);
    }

    @Override
    public void onSuccess(WLResponse wlResponse) {
        submitSuccess(wlResponse);
    }

    @Override
    public void onFailure(WLFailResponse wlFailResponse) {
        submitFailure(wlFailResponse);
    }
}


