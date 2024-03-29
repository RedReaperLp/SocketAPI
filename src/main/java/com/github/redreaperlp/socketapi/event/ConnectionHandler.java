package com.github.redreaperlp.socketapi.event;

import com.github.redreaperlp.socketapi.communication.Connection;

import java.util.HashMap;
import java.util.Map;

public class ConnectionHandler {
    private final Map<String, Class<? extends Connection>> customConnectionClasses = new HashMap<>();
    private static ConnectionHandler instance;

    private ConnectionHandler() {
    }


    public static ConnectionHandler getInstance() {
        if (instance == null) {
            instance = new ConnectionHandler();
        }
        return instance;
    }

    /**
     * Registers a custom connection class
     *
     * @param name  The identifier
     * @param clazz The class
     * @apiNote This is used to identify the connection and differentiate
     * between them to have different actions for each connection
     */
    public void registerCustomConnectionClass(String name, Class<? extends Connection> clazz) {
        if (!hasIdentifier(name)) {
            customConnectionClasses.put(name, clazz);
        }
    }

    /**
     * Checks if the identifier is already registered
     * @param identifier The identifier
     * @return true if the identifier is already registered
     */
    public boolean hasIdentifier(String identifier) {
        return customConnectionClasses.containsKey(identifier);
    }


    /**
     * Returns the registered connection classes
     * @return The registered connection classes
     */
    public Map<String, Class<? extends Connection>> getRegisteredConnectionClasses() {
        return customConnectionClasses;
    }

    /**
     * Returns the connection class for the given identifier
     * @param identifier The identifier
     * @return The connection class
     */
    public Class<? extends Connection> getConnectionClass(String identifier) {
        return customConnectionClasses.get(identifier);
    }
}
