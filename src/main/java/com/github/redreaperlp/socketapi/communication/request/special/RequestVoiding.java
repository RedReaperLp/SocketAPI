package com.github.redreaperlp.socketapi.communication.request.special;

import com.github.redreaperlp.socketapi.communication.RequestManager;
import com.github.redreaperlp.socketapi.communication.request.Request;
import org.json.JSONObject;

/**
 * A Request that does not return a value, it does not even get a response
 */
public abstract class RequestVoiding implements Request {
    private RequestManager manager;
    private JSONObject data;
    @Override
    public RequestManager getManager() {
        return manager;
    }

    @Override
    public void setManager(RequestManager manager) {
        this.manager = manager;
    }

    @Override
    public void setData(JSONObject data) {
        this.data = data;
    }

    @Override
    public JSONObject getData() {
        return data;
    }
}
