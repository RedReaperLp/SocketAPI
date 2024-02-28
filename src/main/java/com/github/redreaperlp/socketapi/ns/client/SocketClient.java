package com.github.redreaperlp.socketapi.ns.client;

import com.github.redreaperlp.socketapi.communication.Connection;
import com.github.redreaperlp.socketapi.communication.ConnectionImpl;
import com.github.redreaperlp.socketapi.communication.handler.IPromisingRequestHandler;
import com.github.redreaperlp.socketapi.communication.handler.IReqHandler;
import com.github.redreaperlp.socketapi.communication.handler.IRequestHandler;
import com.github.redreaperlp.socketapi.communication.request.Request;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestRegister;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestStop;
import com.github.redreaperlp.socketapi.communication.response.Response;
import com.github.redreaperlp.socketapi.ns.NetInstance;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SocketClient implements NetInstance {
    private final int port;
    private final String ip;
    private Connection con;
    private final Object connectionErrorLock = new Object();
    private final Thread connectionErrorThread;

    private String connectionIdentifier;
    private boolean stopped = false;

    private final Map<Class<? extends Request>, IReqHandler> handlers = new HashMap<>();

    private byte[] encryptionKey;

    public SocketClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
        connectionErrorThread = new Thread(this::onConnectionError);
        connectionErrorThread.setName("ConnectionErrorThread");
        connectionErrorThread.start();

        handlers.put(RequestRegister.class, (IPromisingRequestHandler) (req, data) -> {
            if (req.getManager().getNetInstance() instanceof SocketClient client) {
                if (data == null) {
                    req.setResponse(new JSONObject().put("success", false).put("reason", "no data"), 400);
                    return;
                }
                if (data.getBoolean("success")) {
                    client.setConnectionIdentifier(data.getString("identifier"));
                }
                req.setResponse(new JSONObject().put("success", true), 200);
            }
        });
    }

    public void setConnectionIdentifier(String connectionIdentifier) {
        this.connectionIdentifier = connectionIdentifier;
    }

    public boolean start() {
        try {
            Socket socket = new Socket(ip, port);
            con = new ConnectionImpl(socket, this);
            con.incoming();
            con.outgoing();
            con.timeout();

            RequestRegister req = con.getRequestManager().getRequest(RequestRegister.class);
            req.setConnectionIdentifier(connectionIdentifier);
            req.complete();
            if (req.failed() != 200) {
                return false;
            }
            registerHandlers();
            con.ping();
        } catch (IOException e) {
            System.out.println("Failed to connect to " + ip + ":" + port);
            System.out.println("Reason: " + e.getMessage());
            return false;
        }
        return true;
    }

    private Object getConnectionErrorLock() {
        return connectionErrorLock;
    }

    public void onConnectionError() {
        while (true) {
            synchronized (connectionErrorLock) {
                try {
                    connectionErrorLock.wait();
                } catch (InterruptedException e) {
                    System.out.println("ConnectionErrorThread interrupted, stopping...");
                    return;
                }
            }
            con.end();
            System.out.println("Connection error occurred, reconnecting...");
            while (!start()) {
                System.out.println("Failed to reconnect, retrying in 1 second...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("ConnectionErrorThread interrupted, stopping...");
                    return;
                }
            }
        }
    }

    /**
     * Stops the client and sends a stop request to the server, when the server responds, the connection is closed
     */
    @Override
    public void stop() {
        Response res = getRequest(RequestStop.class).complete();
        stopped = true;
        if (connectionErrorThread != null && connectionErrorThread.isAlive()) connectionErrorThread.interrupt();
        con.end();
    }

    /**
     * Creates a new request instance containing the manager
     *
     * @param clazz The request class
     * @param <T>   The request type
     * @return The request instance
     * @apiNote When this method is called, the id is incremented by 1
     */
    public <T extends Request> T getRequest(Class<T> clazz) {
        return con.getRequestManager().getRequest(clazz);
    }

    /**
     * Creates a new request instance containing the manager
     *
     * @param clazz The request class
     * @param <T>   The request type
     * @param id    The request id (if you want to specify it)
     * @return The request instance
     * @apiNote This works too, if the class is not registered, this will not increment the id
     */
    public <T extends Request> T getRequest(Class<T> clazz, long id) {
        return con.getRequestManager().getRequest(clazz, id);
    }

    /**
     * Creates a new request instance containing the manager
     *
     * @param name The request name
     * @return The request instance
     * @apiNote Be careful, <T> is not checked, so you can get a {@link ClassCastException}, only works if the class is registered
     */
    public <T extends Request> T getRequest(String name) {
        return con.getRequestManager().getRequest(name);
    }

    /**
     * Creates a new request instance containing the manager
     *
     * @param name The request name
     * @param id   The request id (if you want to specify it)
     * @return The request instance
     * @apiNote Be careful, <T> is not checked, so you can get a {@link ClassCastException}, only works if the class is registered
     */
    public <T extends Request> T getRequest(String name, long id) {
        return con.getRequestManager().getRequest(name, id);
    }

    public void registerHandlers() {
        handlers.forEach((clazz, handler) -> con.getRequestHandler().registerHandler(clazz, handler));
    }

    @Override
    public void notifyConnectionClosed(Connection con) {
        synchronized (connectionErrorLock) {
            connectionErrorLock.notifyAll();
        }
    }

    @Override
    public byte[] encryptionKey() {
        return encryptionKey;
    }

    @Override
    public void setEncryptionKey(byte[] key) {
        this.encryptionKey = key;
    }

    @Override
    public boolean stopped() {
        return stopped;
    }

    public void registerRequestHandler(Class<? extends Request> clazz, IRequestHandler handler) {
        handlers.put(clazz, handler);
    }

    public void registerPromisingRequestHandler(Class<? extends Request> clazz, IPromisingRequestHandler handler) {
        handlers.put(clazz, handler);
    }

}
