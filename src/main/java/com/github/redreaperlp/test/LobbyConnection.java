package com.github.redreaperlp.test;

import com.github.redreaperlp.socketapi.NetInstance;
import com.github.redreaperlp.socketapi.communication.ConnectionImpl;

import java.net.Socket;
import java.sql.Connection;

public class LobbyConnection extends ConnectionImpl {
    public LobbyConnection(Socket socket, NetInstance netInstance) {
        super(socket, netInstance);
        System.out.println("New connection (LobbyConnection)");
    }
}
