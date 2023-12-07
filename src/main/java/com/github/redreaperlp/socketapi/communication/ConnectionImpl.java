package com.github.redreaperlp.socketapi.communication;

import com.github.redreaperlp.socketapi.ns.NetInstance;

import java.net.Socket;

public class ConnectionImpl extends Connection {
    public ConnectionImpl(Socket socket, NetInstance netInstance) {
        super(socket, netInstance);
    }
}
