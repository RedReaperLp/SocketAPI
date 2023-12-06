package com.github.redreaperlp.socketapi;

import com.github.redreaperlp.socketapi.communication.handler.RequestHandler;

public interface NetInstance {
    RequestHandler getRequestHandler();
}
