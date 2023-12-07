package com.github.redreaperlp.socketapi.ns;

import com.github.redreaperlp.socketapi.communication.Connection;
import com.github.redreaperlp.socketapi.communication.handler.RequestHandler;

public interface NetInstance {
    RequestHandler getRequestHandler();
    void notifyConnectionClosed(Connection con);
}
