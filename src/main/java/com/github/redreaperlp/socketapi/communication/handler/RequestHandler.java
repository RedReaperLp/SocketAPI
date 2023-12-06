package com.github.redreaperlp.socketapi.communication.handler;

import com.github.redreaperlp.socketapi.communication.request.Request;
import com.github.redreaperlp.socketapi.communication.request.special.RequestPromising;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RequestHandler {
    Map<Class<? extends Request>, IRequestHandler> handlers = new HashMap<>();
    Map<Class<? extends RequestPromising>, IPromisingRequestHandler> promisingHandlers = new HashMap<>();

    /**
     * Registers a handler for a request
     *
     * @param clazz   The request class
     * @param handler The handler
     * @apiNote This handler will be called when the request arrives
     */
    public void registerHandler(Class<? extends Request> clazz, IRequestHandler handler) {
        handlers.put(clazz, handler);
    }

    /**
     * Registers a handler for a promising request
     *
     * @param clazz   The request class
     * @param handler The handler
     * @apiNote This handler will be called when the request arrives
     */
    public void registerPromisingHandler(Class<? extends RequestPromising> clazz, IPromisingRequestHandler handler) {
        promisingHandlers.put(clazz, handler);
    }

    /**
     * Handles a request
     *
     * @param request The request to handle
     */
    public void handleRequest(Request request, JSONObject data) {
        if (handlers.containsKey(request.getClass())) {
            handlers.get(request.getClass()).handleRequest(request, data);
        } else if (request instanceof RequestPromising promising) {
            if (promisingHandlers.containsKey(request.getClass())) {
                promisingHandlers.get(request.getClass()).handleRequest(promising, data);
            }
        }
    }
}
