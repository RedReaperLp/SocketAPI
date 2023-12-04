package com.github.redreaperlp.socketapi.server;

import com.github.redreaperlp.socketapi.NetInstance;
import com.github.redreaperlp.socketapi.communication.Connection;
import com.github.redreaperlp.socketapi.communication.ConnectionImpl;
import com.github.redreaperlp.socketapi.communication.response.Response;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SocketServer implements NetInstance {
    private int port;
    private Thread incomingThread;
    private final Map<String, Class<? extends Connection>> customConnectionClasses = new HashMap<>();

    public void registerCustomConnectionClass(String name, Class<? extends Connection> clazz) {
        if (!customConnectionClasses.containsKey(name)) {
            customConnectionClasses.put(name, clazz);
        }
    }

    public SocketServer(int port) {
        this.port = port;
    }

    public void start() {
        incomingThread = new Thread(() -> {
            ServerSocket serverSocket;
            try {
                serverSocket = new ServerSocket(port);
                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("New connection from " + socket.getInetAddress().getHostAddress());
                    if (customConnectionClasses.isEmpty()) {
                        Connection con = new ConnectionImpl(socket, this);
                        con.incoming();
                        con.outgoing();
                        con.timeout();
                    } else {
                        new Thread(() -> {
                            try {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                String line = reader.readLine();
                                if (line != null) {
                                    JSONObject jsonObject = new JSONObject(line);
                                    JSONObject data = jsonObject.getJSONObject("data");
                                    System.out.println(data);
                                    if (data.has("identifier")) {
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
                                        } else {
                                            System.out.println("No custom connection class found for identifier " + identifier);
                                            Connection con = new ConnectionImpl(socket, this);
                                            con.outgoing();
                                            Response res = con.getRequestManager().getRequest(Response.class, id);
                                            res.setStatus(404);
                                            res.setID(id);
                                            res.setData(new JSONObject().put("reason", "No custom connection class found for identifier \"" + identifier + "\""));
                                            res.queue();
                                            con.end();
                                            return;
                                        }
                                    } else {
                                        Connection con = new ConnectionImpl(socket, this);
                                        con.outgoing();
                                        Response res = con.getRequestManager().getRequest(Response.class, jsonObject.getInt("id"));
                                        res.setStatus(400);
                                        res.setData(new JSONObject().put("reason", "No identifier found"));
                                        res.queue();
                                        System.out.println("No identifier found");
                                        con.end();
                                    }
                                }
                            } catch (IOException | NoSuchMethodException | InstantiationException |
                                     IllegalAccessException | InvocationTargetException e) {
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

    public boolean hasIdentifier(String identifier) {
        return customConnectionClasses.containsKey(identifier);
    }

    public Map<String, Class<? extends Connection>> getRegisteredConnectionClasses() {
        return customConnectionClasses;
    }
}
