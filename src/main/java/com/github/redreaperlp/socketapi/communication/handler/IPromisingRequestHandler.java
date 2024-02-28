package com.github.redreaperlp.socketapi.communication.handler;

import com.github.redreaperlp.socketapi.communication.request.special.RequestPromising;
import org.json.JSONObject;

public interface IPromisingRequestHandler extends IReqHandler {
    /**
     * Handles a request
     * @param request The request to handle
     *                @param res The future response
     */
    void handleRequest(RequestPromising request, JSONObject res);
}
