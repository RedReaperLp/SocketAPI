package com.github.redreaperlp.socketapi.client;

import com.github.redreaperlp.socketapi.NetInstance;
import com.github.redreaperlp.socketapi.communication.Connection;
import com.github.redreaperlp.socketapi.communication.ConnectionImpl;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestPing;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestRegister;

import java.io.IOException;
import java.net.Socket;

public class Client implements NetInstance {
    private int port;
    private String ip;

    private String connectionIdentifier;

    public Client(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void setConnectionIdentifier(String connectionIdentifier) {
        this.connectionIdentifier = connectionIdentifier;
    }

    public void start() {
        try {
            Socket socket = new Socket(ip, port);
            Connection con = new ConnectionImpl(socket, this);
            con.incoming();
            con.outgoing();
            con.timeout();

            RequestRegister req = con.getRequestManager().getRequest(RequestRegister.class);
            req.setConnectionIdentifier(connectionIdentifier);
            req.complete();
            if (req.failed() != 200) {
                System.out.println("Failed to register: " + req.getResponse().getData().getString("reason"));
                return;
            }
            while (true) {
                Thread.sleep(2000);
                con.getRequestManager().getRequest(RequestPing.class).complete();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
