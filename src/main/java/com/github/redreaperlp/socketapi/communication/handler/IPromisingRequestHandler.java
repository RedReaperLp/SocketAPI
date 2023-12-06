package com.github.redreaperlp.socketapi.communication.handler;

import com.github.redreaperlp.socketapi.communication.request.special.RequestPromising;
import org.json.JSONObject;

public interface IPromisingRequestHandler {
    /**
     * Handles a request
     * @param request The request to handle
     */
    void handleRequest(RequestPromising request, JSONObject res);
}
