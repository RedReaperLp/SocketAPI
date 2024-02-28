package com.github.redreaperlp.socketapi.communication.handler;

import com.github.redreaperlp.socketapi.communication.request.Request;
import com.github.redreaperlp.socketapi.communication.request.special.RequestPromising;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestHandler {
    Map<Class<? extends Request>, List<IRequestHandler>> handlers = new HashMap<>();
    Map<Class<? extends RequestPromising>, List<IPromisingRequestHandler>> promisingHandlers = new HashMap<>();

    /**
     * Registers a handler for a request
     *
     * @param clazz   The request class
     * @param handler The handler
     * @apiNote This handler will be called when the request arrives
     */
    public void registerHandler(Class<? extends Request> clazz, IReqHandler handler) {
        if (handler instanceof IRequestHandler reqHandler) {
            if (handlers.containsKey(clazz)) {
                handlers.get(clazz).add(reqHandler);
                return;
            }
            List<IRequestHandler> list = new ArrayList<>();
            list.add(reqHandler);
            handlers.put(clazz, list);
        } else if (handler instanceof IPromisingRequestHandler reqHandler) {
            if (clazz.getSuperclass().equals(RequestPromising.class)) {
                registerPromisingHandler((Class<? extends RequestPromising>) clazz, reqHandler);
            }
        }
    }

    /**
     * Registers a handler for a promising request
     *
     * @param clazz   The request class
     * @param handler The handler
     * @apiNote This handler will be called when the request arrives
     */
    public void registerPromisingHandler(Class<? extends RequestPromising> clazz, IPromisingRequestHandler handler) {
        if (promisingHandlers.containsKey(clazz)) {
            promisingHandlers.get(clazz).add(handler);
            return;
        }
        List<IPromisingRequestHandler> list = new ArrayList<>();
        list.add(handler);
        promisingHandlers.put(clazz, list);
    }

    /**
     * Unregisters a handler for a request
     *
     * @param clazz   The request class
     * @param handler The handler
     */
    public void unregisterHandler(Class<? extends Request> clazz, IRequestHandler handler) {
        if (handlers.containsKey(clazz)) {
            handlers.get(clazz).remove(handler);
        }
    }

    /**
     * Unregisters a handler for a promising request
     *
     * @param clazz   The request class
     * @param handler The handler
     */
    public void unregisterPromisingHandler(Class<? extends RequestPromising> clazz, IPromisingRequestHandler handler) {
        if (promisingHandlers.containsKey(clazz)) {
            promisingHandlers.get(clazz).remove(handler);
        }
    }

    /**
     * Handles a request
     *
     * @param request The request to handle
     *                @param data The data to handle
     */
    public void handleRequest(Request request, JSONObject data) {
        if (handlers.containsKey(request.getClass())) {
            handlers.get(request.getClass()).forEach(h -> h.handleRequest(request, data));
        }
        if (request instanceof RequestPromising promising) {
            if (promisingHandlers.containsKey(request.getClass())) {
                promisingHandlers.get(request.getClass()).forEach(h -> h.handleRequest(promising, data));
            }
        }
    }
}
