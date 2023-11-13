package com.github.redreaperlp.socketapi.communication.request;

import com.github.redreaperlp.socketapi.communication.RequestManager;
import org.json.JSONObject;

public interface Request {
    default void queue() {
        pack();
        getManager().queue(this);
    }

    RequestManager getManager();

    void setManager(RequestManager manager);

    String getType();

    void setData(JSONObject data);

    JSONObject getData();

    void pack();
}
