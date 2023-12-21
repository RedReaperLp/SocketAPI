package com.github.redreaperlp;

import com.github.redreaperlp.socketapi.communication.RequestManager;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestPing;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestRegister;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestStop;
import com.github.redreaperlp.socketapi.event.ConnectionHandler;
import com.github.redreaperlp.socketapi.ns.client.SocketClient;
import com.github.redreaperlp.socketapi.ns.server.SocketServer;
import com.github.redreaperlp.test.GameConnection;
import com.github.redreaperlp.test.LobbyConnection;

public class Main {
    public static void main(String[] args) {
        RequestManager.registerRequest(RequestPing.name, RequestPing.class);
        RequestManager.registerRequest(RequestRegister.name, RequestRegister.class);
        RequestManager.registerRequest(RequestStop.name, RequestStop.class);

        ConnectionHandler h = ConnectionHandler.getInstance();
        h.registerCustomConnectionClass("game", GameConnection.class);
        h.registerCustomConnectionClass("lobby", LobbyConnection.class);

        if (args.length > 0) {
            switch (args[0]) {
                case "server" -> {
                    SocketServer socketServer = new SocketServer(800);
                    socketServer.start();
                    socketServer.getRequestHandler().registerHandler(RequestPing.class, (req, data) -> {
                        System.out.println("Ping from " + req.getManager().getConnection().getSocket().getInetAddress().getHostAddress());
                    });
                    socketServer.getRequestHandler().registerPromisingHandler(RequestRegister.class, (req, data) -> {
                        System.out.println("Register from " + req.getManager().getConnection().getSocket().getInetAddress().getHostAddress());
                    });
                }
                case "client" -> {
                    SocketClient socketClient = new SocketClient("localhost", 800);
                    socketClient.setConnectionIdentifier("lobby");
                    socketClient.start();
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    socketClient.stop();
                }
            }
        }
    }
}

//Todo: Think about making the RequestHandler a singleton (Meaning: Only one instance of it can exist)
//Todo: Think about making the SocketServer#customConnectionClasses a singleton