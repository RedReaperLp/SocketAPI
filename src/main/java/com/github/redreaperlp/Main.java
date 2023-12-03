package com.github.redreaperlp;

import com.github.redreaperlp.socketapi.client.SocketClient;
import com.github.redreaperlp.socketapi.communication.RequestManager;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestPing;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestRegister;
import com.github.redreaperlp.socketapi.server.SocketServer;
import com.github.redreaperlp.test.GameConnection;
import com.github.redreaperlp.test.LobbyConnection;

public class Main {
    public static void main(String[] args) {
        RequestManager.registerRequest("ping", RequestPing.class);
        RequestManager.registerRequest("register", RequestRegister.class);

        if (args.length > 0) {
            switch (args[0]){
                case "server" -> {
                    SocketServer socketServer = new SocketServer(800);
                    socketServer.registerCustomConnectionClass("game", GameConnection.class);
                    socketServer.registerCustomConnectionClass("lobby", LobbyConnection.class);
                    socketServer.start();
                }
                case "client" -> {
                    SocketClient socketClient = new SocketClient("localhost", 800);
                    socketClient.setConnectionIdentifier("lobby");
                    socketClient.start();
                }
            }
        }
    }
}