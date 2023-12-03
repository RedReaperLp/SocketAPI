package com.github.redreaperlp.socketapi.server;

import com.github.redreaperlp.socketapi.NetInstance;
import com.github.redreaperlp.socketapi.communication.Connection;
import com.github.redreaperlp.socketapi.communication.ConnectionImpl;
import com.github.redreaperlp.socketapi.communication.request.Request;
import com.github.redreaperlp.socketapi.communication.response.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocketServer implements NetInstance {
    private int port;
    private Thread incomingThread;
    private Connection con;
    private volatile boolean running = true;
    private final Map<String, Class<? extends Connection>> customConnectionClasses = new HashMap<>();
    private final List<Connection> connections = new ArrayList<>();

    /**
     * Registers a custom connection class for a specific identifier
     *
     * @param name  The identifier
     * @param clazz The class
     * @apiNote This method should be called before {@link #start()}
     */
    public void registerCustomConnectionClass(String name, Class<? extends Connection> clazz) {
        if (!customConnectionClasses.containsKey(name)) {
            customConnectionClasses.put(name, clazz);
        }
    }

    public SocketServer(int port) {
        this.port = port;
    }

    /**
     * Starts the server
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                Socket socket = serverSocket.accept();
                handleConnection(socket);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleConnection(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line = reader.readLine();
            if (line != null) {
                JSONObject jsonObject = new JSONObject(line);
                JSONObject data = jsonObject.getJSONObject("data");
                System.out.println(data);

                if (data.has("identifier")) {
                    handleCustomConnection(socket, jsonObject, data);
                } else {
                    handleDefaultConnection(socket, jsonObject);
                }
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleCustomConnection(Socket socket, JSONObject jsonObject, JSONObject data) {
        try {
            String identifier = data.getString("identifier");
            int id = jsonObject.getInt("id");

            if (customConnectionClasses.containsKey(identifier)) {
                Connection con = customConnectionClasses.get(identifier).getDeclaredConstructor(Socket.class, NetInstance.class).newInstance(socket, this);
                Response res = con.getRequestManager().getRequest(Response.class, id);
                con.incoming();
                con.outgoing();
                res.setStatus(200);
                res.setID(id);
                res.setData(new JSONObject().put("identifier", identifier));
                res.queue();
                connections.add(con);
            } else {
                System.out.println("No custom connection class found for identifier " + identifier);
                handleDefaultConnectionError(socket, id, "No custom connection class found for identifier \"" + identifier + "\"");
            }
        } catch (ReflectiveOperationException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleDefaultConnection(Socket socket, JSONObject jsonObject) {
        try {
            Connection con = new ConnectionImpl(socket, this);
            Response res = con.getRequestManager().getRequest(Response.class, jsonObject.getInt("id"));
            if (customConnectionClasses.isEmpty()) {
                con.incoming();
                con.outgoing();
                res.setStatus(200);
                res.setID(jsonObject.getInt("id"));
                res.setData(new JSONObject().put("identifier", "default"));
                res.queue();
                connections.add(con);
            } else {
                con.outgoing();
                res.setStatus(400);
                res.setData(new JSONObject().put("reason", "No identifier found"));
                res.queue();
                System.out.println("No identifier found");
                con.end();
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleDefaultConnectionError(Socket socket, int id, String reason) {
        try {
            Connection con = new ConnectionImpl(socket, this);
            con.outgoing();
            Response res = con.getRequestManager().getRequest(Response.class, id);
            res.setStatus(404);
            res.setID(id);
            res.setData(new JSONObject().put("reason", reason));
            res.queue();
            con.end();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    public boolean hasIdentifier(String identifier) {
        return customConnectionClasses.containsKey(identifier);
    }

    public Map<String, Class<? extends Connection>> getRegisteredConnectionClasses() {
        return customConnectionClasses;
    }

    @Override
    public Request getRequest(Class<? extends Request> clazz) {
        return con.getRequestManager().getRequest(clazz);
    }

    public void stop() {
        running = false;
        if (incomingThread != null && incomingThread.isAlive()) {
            incomingThread.interrupt();
        }
    }
}
