package com.github.redreaperlp.socketapi.communication.request.requests;

import com.github.redreaperlp.socketapi.communication.request.special.RequestPromising;
import org.json.JSONObject;

public class RequestPing extends RequestPromising {
    private final String identifier = "ping";

    public RequestPing(long id) {
        super(id);
    }

    @Override
    public void validateRequest() {
        setResponse(new JSONObject().put("pong", true), 200);
    }

    @Override
    public void validateResponse() {
    }

    @Override
    public String getType() {
        return identifier;
    }
}
