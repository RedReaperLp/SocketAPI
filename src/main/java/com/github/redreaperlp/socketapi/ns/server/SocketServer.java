package com.github.redreaperlp.socketapi.ns.server;

import com.github.redreaperlp.socketapi.communication.Connection;
import com.github.redreaperlp.socketapi.communication.ConnectionImpl;
import com.github.redreaperlp.socketapi.communication.handler.RequestHandler;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestPing;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestRegister;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestStop;
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
import java.util.List;

public class SocketServer implements NetInstance {
    private int port;
    private Thread incomingThread;
    private RequestHandler requestHandler = new RequestHandler();

    private List<Connection> connections = new ArrayList<>();


    public SocketServer(int port) {
        this.port = port;
        requestHandler.registerPromisingHandler(RequestPing.class, (req, data) -> {
            req.setResponse(new JSONObject().put("pong", true), 200);
        });
        requestHandler.registerPromisingHandler(RequestStop.class, (req, data) -> {
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
        });
        //TODO: Add a way to execute something when the message is sent like register(...).finally(() -> {})
    }

    /**
     * Starts the server and listens for incoming connections
     */
    public void start() {
        incomingThread = new Thread(() -> {
            ServerSocket serverSocket;
            try {
                serverSocket = new ServerSocket(port);
                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("New connection from " + socket.getInetAddress().getHostAddress());
                    if (ConnectionHandler.getInstance().getRegisteredConnectionClasses().isEmpty()) {
                        Connection con = new ConnectionImpl(socket, this);
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
                                    JSONObject jsonObject = new JSONObject(line);
                                    JSONObject data = jsonObject.getJSONObject("data");

                                    int id = jsonObject.getInt("id");
                                    Connection con = new ConnectionImpl(socket, this);
                                    con.outgoing();
                                    Response fallback = con.getRequestManager().getRequest(Response.class, id);
                                    requestHandler.handleRequest(con.getRequestManager().getRequest(RequestRegister.class, id), data);
                                    if (data.has("identifier")) {
                                        String identifier = data.getString("identifier");
                                        if (ConnectionHandler.getInstance().hasIdentifier(identifier)) {
                                            con.end(false);
                                            Connection customCon = ConnectionHandler.getInstance().getConnectionClass(identifier)
                                                    .getDeclaredConstructor(Socket.class, NetInstance.class)
                                                    .newInstance(socket, this);
                                            customCon.incoming();
                                            customCon.outgoing();
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
     * Gets the request handler
     *
     * @return The request handler
     * @apiNote The request handler is used to register requests and handle/modifiy them
     */
    public RequestHandler getRequestHandler() {
        return requestHandler;
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
}
