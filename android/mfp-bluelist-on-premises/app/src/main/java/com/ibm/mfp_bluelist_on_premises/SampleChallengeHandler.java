package com.ibm.mfp_bluelist_on_premises;

import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLProcedureInvocationData;
import com.worklight.wlclient.api.WLRequestOptions;
import com.worklight.wlclient.api.WLResponse;
import com.worklight.wlclient.api.challengehandler.ChallengeHandler;

import org.json.JSONException;

/**
 * Created by drcariel on 3/12/2015.
 */
public class SampleChallengeHandler extends ChallengeHandler {
    public String UserName;
    public String UserPassword;
    public String AdapterName;
    public String ScopeRealm;

    public SampleChallengeHandler(String realm) {
        super(realm);
    }

    @Override
    public boolean isCustomResponse(WLResponse response) {
        try {
            if(response!= null &&
                    response.getResponseJSON()!=null &&
                    response.getResponseJSON().isNull("authRequired") != true &&
                    response.getResponseJSON().getBoolean("authRequired") == true){
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            // TODO log
        }
        return false;
    }

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

