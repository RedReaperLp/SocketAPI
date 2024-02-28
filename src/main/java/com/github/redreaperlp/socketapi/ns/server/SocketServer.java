package com.github.redreaperlp.socketapi.ns.server;

import com.github.redreaperlp.socketapi.communication.Connection;
import com.github.redreaperlp.socketapi.communication.ConnectionImpl;
import com.github.redreaperlp.socketapi.communication.handler.IPromisingRequestHandler;
import com.github.redreaperlp.socketapi.communication.handler.IReqHandler;
import com.github.redreaperlp.socketapi.communication.handler.IRequestHandler;
import com.github.redreaperlp.socketapi.communication.request.Request;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestPing;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestRegister;
import com.github.redreaperlp.socketapi.communication.request.special.RequestPromising;
import com.github.redreaperlp.socketapi.communication.response.Response;
import com.github.redreaperlp.socketapi.event.ConnectionHandler;
import com.github.redreaperlp.socketapi.ns.NetInstance;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocketServer implements NetInstance {
    private final int port;
    private Thread incomingThread;
    private final List<Connection> connections = new ArrayList<>();
    private byte[] encryptionKey;

    private final Map<Class<? extends Request>, IReqHandler> handlers = new HashMap<>();
    private boolean stopped = false;


    public SocketServer(int port) {
        this.port = port;
        //TODO: Add a way to execute something when the message is sent like register(...).finally(() -> {})
    }

    IPromisingRequestHandler pingHandler = (req, data) -> {
        req.setResponse(new JSONObject().put("pong", true), 200);
    };

    IPromisingRequestHandler requestHandler = (req, data) -> {
        System.out.println("Got stop request");
        req.setResponse(new JSONObject().put("bye", true), 200);
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            req.getManager().getConnection().connectionError();
        }).start();
    };

    /**
     * Starts the server and listens for incoming connections
     */
    public void start() {
        incomingThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("New connection from " + socket.getInetAddress().getHostAddress());
                    if (ConnectionHandler.getInstance().getRegisteredConnectionClasses().isEmpty()) {
                        Connection con = new ConnectionImpl(socket, this);
                        con.getRequestHandler().registerPromisingHandler(RequestPing.class, pingHandler);
                        handlers.forEach((clazz, handler) -> con.getRequestHandler().registerHandler(clazz, handler));
                        con.incoming();
                        con.outgoing();
                        con.timeout();
                        con.notifier(con);
                        connections.add(con);
                    } else {
                        new Thread(() -> {
                            try {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                String line = reader.readLine();
                                if (line != null) {
                                    if (usesEncrytion()) {
                                        line = decrypt(line);
                                    }
                                    JSONObject jsonObject = new JSONObject(line);
                                    JSONObject data = jsonObject.getJSONObject("data");

                                    int id = jsonObject.getInt("id");
                                    Connection con = new ConnectionImpl(socket, this);
                                    con.getRequestHandler().registerPromisingHandler(RequestPing.class, pingHandler);
                                    con.outgoing();
                                    Response fallback = con.getRequestManager().getRequest(Response.class, id);
                                    con.getRequestHandler().handleRequest(con.getRequestManager().getRequest(RequestRegister.class, id), data);
                                    if (data.has("identifier")) {
                                        String identifier = data.getString("identifier");
                                        if (ConnectionHandler.getInstance().hasIdentifier(identifier)) {
                                            con.end(false);
                                            Connection customCon = ConnectionHandler.getInstance().getConnectionClass(identifier)
                                                    .getDeclaredConstructor(Socket.class, NetInstance.class)
                                                    .newInstance(socket, this);
                                            customCon.incoming();
                                            customCon.outgoing();
                                            customCon.getRequestHandler().registerPromisingHandler(RequestPing.class, pingHandler);
                                            handlers.forEach((clazz, handler) -> customCon.getRequestHandler().registerHandler(clazz, handler));
                                            Response res = customCon.getRequestManager().getRequest(Response.class, id);
                                            res.setStatus(200);
                                            res.setID(id);
                                            res.setData(new JSONObject().put("identifier", identifier));
                                            res.queue();
                                            connections.add(customCon);
                                            return;
                                        }
                                        fallback.setStatus(404);
                                        fallback.setData(new JSONObject().put("reason", "No custom connection class found for identifier \"" + identifier + "\""));
                                    } else {
                                        fallback.setStatus(400);
                                        fallback.setData(new JSONObject().put("reason", "No identifier found"));
                                    }
                                    fallback.queue();
                                    Thread.sleep(1000);
                                    con.end();
                                }
                            } catch (IOException | NoSuchMethodException | InstantiationException |
                                     IllegalAccessException | InvocationTargetException | InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }).start();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        incomingThread.start();
    }

    /**
     * Removes a connection from the list
     * @param con The connection to remove
     */
    public void removeConnection(Connection con) {
        connections.remove(con);
    }

    /**
     * Gets all connections of a specific type
     * @param clazz The connection class
     * @return A list of connections
     * @param <T> The connection type
     */
    public <T extends Connection> List<T> getConnections(Class<T> clazz) {
        List<T> list = new ArrayList<>();
        for (Connection con : connections) {
            if (clazz.isInstance(con)) {
                list.add((T) con);
            }
        }
        return list;
    }

    @Override
    public void notifyConnectionClosed(Connection con) {
        con.end();
        System.out.println("Connection closed: " + con.getSocket().getInetAddress().getHostAddress());
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
    public void stop() {
        stopped = true;
        for (Connection con : connections) {
            con.end();
        }
    }

    @Override
    public boolean stopped() {
        return stopped;
    }

    public void registerHandler(Class<? extends Request> requestClass, IRequestHandler handler) {
        handlers.put(requestClass, handler);
    }

    public void registerPromisingHandler(Class<? extends RequestPromising> req, IPromisingRequestHandler handler) {
        handlers.put(req, handler);
    }
}
