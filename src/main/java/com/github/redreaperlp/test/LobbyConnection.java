package com.github.redreaperlp.test;

import com.github.redreaperlp.socketapi.ns.NetInstance;
import com.github.redreaperlp.socketapi.communication.ConnectionImpl;

import java.net.Socket;

public class LobbyConnection extends ConnectionImpl {
    public LobbyConnection(Socket socket, NetInstance netInstance) {
        super(socket, netInstance);
        System.out.println("New connection (LobbyConnection)");
    }
}
