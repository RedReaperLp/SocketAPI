package com.github.redreaperlp;

import com.github.redreaperlp.socketapi.client.Client;
import com.github.redreaperlp.socketapi.communication.RequestManager;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestPing;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestRegister;
import com.github.redreaperlp.socketapi.server.Server;
import com.github.redreaperlp.test.GameConnection;
import com.github.redreaperlp.test.LobbyConnection;

public class Main {
    public static void main(String[] args) {
        RequestManager.registerRequest("ping", RequestPing.class);
        RequestManager.registerRequest("register", RequestRegister.class);

        if (args.length > 0) {
            switch (args[0]){
                case "server" -> {
                    Server server = new Server(800);
                    server.registerCustomConnectionClass("game", GameConnection.class);
                    server.registerCustomConnectionClass("lobby", LobbyConnection.class);
                    server.start();
                }
                case "client" -> {
                    Client client = new Client("localhost", 800);
                    client.setConnectionIdentifier("lobby");
                    client.start();
                }
            }
        }
    }
}