package com.github.redreaperlp.socketapi.communication.request.requests;

import com.github.redreaperlp.socketapi.communication.request.special.RequestPromising;

public class RequestPing extends RequestPromising {
    public static final String name = "ping";

    public RequestPing(long id) {
        super(id);
    }

    @Override
    public String getName() {
        return name;
    }
}
