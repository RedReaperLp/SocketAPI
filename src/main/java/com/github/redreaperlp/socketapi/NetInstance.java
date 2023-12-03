package com.github.redreaperlp.socketapi;

import com.github.redreaperlp.socketapi.communication.request.Request;

public interface NetInstance {
    Request getRequest(Class<? extends Request> clazz);
}
