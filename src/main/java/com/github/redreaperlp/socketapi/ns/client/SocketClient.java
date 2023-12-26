package com.github.redreaperlp.socketapi.ns.client;

import com.github.redreaperlp.socketapi.communication.Connection;
import com.github.redreaperlp.socketapi.communication.ConnectionImpl;
import com.github.redreaperlp.socketapi.communication.handler.RequestHandler;
import com.github.redreaperlp.socketapi.communication.request.Request;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestRegister;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestStop;
import com.github.redreaperlp.socketapi.communication.response.Response;
import com.github.redreaperlp.socketapi.event.ConnectionHandler;
import com.github.redreaperlp.socketapi.ns.NetInstance;
import com.github.redreaperlp.socketapi.ns.server.SocketServer;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;

public class SocketClient implements NetInstance {
    private int port;
    private String ip;
    private Connection con;
    private RequestHandler requestHandler = new RequestHandler();

    private Object connectionErrorLock = new Object();
    private Thread connectionErrorThread;

    private String connectionIdentifier;

    public SocketClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
        registerHandlers();
        connectionErrorThread = new Thread(this::onConnectionError);
        connectionErrorThread.setName("ConnectionErrorThread");
        connectionErrorThread.start();
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
                System.out.println("Failed to register: " + req.getResponse().getData().getString("reason"));
                return false;
            }
            con.ping();
        } catch (IOException e) {
            System.out.println("Failed to connect to " + ip + ":" + port);
            e.printStackTrace();
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

    public void stop() {
        Response res = getRequest(RequestStop.class).complete();
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

    @Override
    public RequestHandler getRequestHandler() {
        return requestHandler;
    }

    public void registerHandlers() {
        requestHandler.registerPromisingHandler(RequestRegister.class, (req, data) -> {
            if (req.getManager().getNetInstance() instanceof SocketServer server) {
                if (data == null) {
                    req.setResponse(new JSONObject().put("success", false).put("reason", "no data"), 400);
                    return;
                }
                if (!ConnectionHandler.getInstance().getRegisteredConnectionClasses().isEmpty()) {
                    if (!ConnectionHandler.getInstance().hasIdentifier(data.get("identifier").toString())) {
                        req.setResponse(new JSONObject().put("success", false).put("reason", "identifier not found"), 404);
                    }
                }
                req.setResponse(new JSONObject().put("success", true), 200);
            }
        });
    }

    @Override
    public void notifyConnectionClosed(Connection con) {
        synchronized (connectionErrorLock) {
            connectionErrorLock.notifyAll();
        }
    }
}
