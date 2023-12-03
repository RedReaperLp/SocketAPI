package com.github.redreaperlp.socketapi.communication.request.requests;

import com.github.redreaperlp.socketapi.client.SocketClient;
import com.github.redreaperlp.socketapi.communication.request.special.RequestVoiding;

public class RequestDisconnect extends RequestVoiding {
    String identifier = "disconnect";

    @Override
    public String getType() {
        return identifier;
    }

    @Override
    public void queue() {
        super.queue();
        if (getManager().getNetInstance() instanceof SocketClient sc) {
            sc.stop();
        }
    }

    @Override
    public void pack() {

    }
}
