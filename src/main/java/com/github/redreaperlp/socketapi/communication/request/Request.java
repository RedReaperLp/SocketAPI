package com.github.redreaperlp.socketapi.communication.request;

import com.github.redreaperlp.socketapi.communication.RequestManager;
import org.json.JSONObject;

public interface Request {
    //TODO: Make thread wait until the request is sent if a boolean is set to true
    default void queue() {
        pack();
        getManager().queue(this);
    }

    RequestManager getManager();

    void setManager(RequestManager manager);

    String getName();

    void setData(JSONObject data);

    JSONObject getData();

    void pack();
}
