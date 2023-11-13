package com.github.redreaperlp.test;

import com.github.redreaperlp.socketapi.NetInstance;
import com.github.redreaperlp.socketapi.communication.ConnectionImpl;

import java.net.Socket;

public class GameConnection extends ConnectionImpl {
    public GameConnection(Socket socket, NetInstance netInstance) {
        super(socket, netInstance);
        System.out.println("New connection (GameConnection)");
    }
}
