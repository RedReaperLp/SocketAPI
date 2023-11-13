package com.github.redreaperlp.socketapi.communication;

import com.github.redreaperlp.socketapi.NetInstance;
import com.github.redreaperlp.socketapi.communication.request.Request;
import com.github.redreaperlp.socketapi.communication.request.special.RequestPromising;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class RequestManager {
    private Connection connection;
    private NetInstance netInstance;
    private long idCounter = 0;
    private static final Map<String, Class<? extends Request>> requests = new HashMap<>();

    /**
     * Registers a request
     *
     * @param name    The name of the request
     * @param request The request class
     */
    public static void registerRequest(String name, Class<? extends Request> request) {
        if (requests.containsKey(name)) return;
        requests.put(name, request);
    }

    public RequestManager(NetInstance netInstance) {
        this.netInstance = netInstance;
    }

    public void queue(Request request) {
        connection.queue(request);
    }

    /**
     * Creates a new request instance containing the manager
     * @param clazz The request class
     * @return The request instance
     * @param <T> The request type
     * @apiNote When this method is called, the id is incremented by 1
     */
    public <T extends Request> T getRequest(Class<T> clazz) {
        return getRequest(clazz, idCounter++);
    }

    /**
     * Creates a new request instance containing the manager
     *
     * @param clazz The request class
     * @param <T>   The request type
     * @return The request instance
     * @apiNote This works too, if the class is not registered, this will not increment the id
     */
    public <T extends Request> T getRequest(Class<T> clazz, long id) {
        try {
            if (clazz.getSuperclass().equals(RequestPromising.class)) {
                RequestPromising req = (RequestPromising) clazz.getDeclaredConstructor(long.class).newInstance(id);
                req.setManager(this);
                return (T) req;
            }

            T req = clazz.getDeclaredConstructor().newInstance();
            req.setManager(this);
            return req;
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends Request> T getRequest(String name) {
        return getRequest(name, -1);
    }

    /**
     * Creates a new request instance containing the manager
     *
     * @param name The request name
     * @return The request instance
     * @apiNote Be careful, <T> is not checked, so you can get a {@link ClassCastException}, only works if the class is registered
     */
    public <T extends Request> T getRequest(String name, long id) {
        Class<? extends Request> clazz = getRequestClass(name);
        if (clazz == null) return null;
        return (T) getRequest(clazz, id);
    }

    /**
     * Gets the request class by name
     *
     * @param name The name of the request
     * @return The request class
     * @apiNote Returns null if the request is not registered
     */
    public static Class<? extends Request> getRequestClass(String name) {
        if (!requests.containsKey(name)) return null;
        return requests.get(name);
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public NetInstance getNetInstance() {
        return netInstance;
    }
}
