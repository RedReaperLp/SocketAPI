package com.github.redreaperlp.socketapi.communication.handler;

import com.github.redreaperlp.socketapi.communication.request.Request;
import com.github.redreaperlp.socketapi.communication.response.Response;
import org.json.JSONObject;

public interface IRequestHandler {
    /**
     * Handles a request
     * @param request The request to handle
     */
    void handleRequest(Request request, JSONObject res);
}
