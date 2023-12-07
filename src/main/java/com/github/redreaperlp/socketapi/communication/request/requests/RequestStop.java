package com.github.redreaperlp.socketapi.communication.request.requests;

import com.github.redreaperlp.socketapi.communication.request.special.RequestPromising;

public class RequestStop extends RequestPromising {
    public static final String name = "stop";

    public RequestStop(long id) {
        super(id);
    }

    @Override
    public String getName() {
        return name;
    }
}
