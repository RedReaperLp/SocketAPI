package com.github.redreaperlp.socketapi.client;

import com.github.redreaperlp.socketapi.NetInstance;
import com.github.redreaperlp.socketapi.communication.Connection;
import com.github.redreaperlp.socketapi.communication.ConnectionImpl;
import com.github.redreaperlp.socketapi.communication.request.Request;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestRegister;

import java.io.IOException;
import java.net.Socket;

public class SocketClient implements NetInstance {
    private int port;
    private String ip;

    private String connectionIdentifier;

    private Connection con;
    private volatile boolean running = true;

    /**
     * Creates a new SocketClient
     *
     * @param ip   The IP of the server
     * @param port The port of the server
     */
    public SocketClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    /**
     * Sets the identifier of the connection
     *
     * @param connectionIdentifier The identifier of the connection, this is used to identify the connection on the server
     */
    public void setConnectionIdentifier(String connectionIdentifier) {
        this.connectionIdentifier = connectionIdentifier;
    }

    /**
     * Starts the client
     */
    public void start() {
        Socket socket = null;
        boolean success = false;
        while (!success) {
            try {
                socket = new Socket(ip, port);
                success = true;
            } catch (IOException e) {
                System.out.println("Failed to connect to " + ip + ":" + port);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interruptedException) {
                    System.out.println("Interrupted while waiting for connection");
                }
            }
        }
        con = new ConnectionImpl(socket, this);
        con.incoming();
        con.outgoing();
        con.timeout();
        con.ping();

        RequestRegister req = con.getRequestManager().getRequest(RequestRegister.class);
        req.setConnectionIdentifier(connectionIdentifier);
        req.complete();
        if (req.failed() != 200) {
            System.out.println("Failed to register: " + req.getResponse().getData().getString("reason"));
            stop();
        }
    }

    public synchronized void restart() {
        if (!running) return;
        if (con != null) con.end();
        System.out.println("Restarting");
        start();
    }

    @Override
    public Request getRequest(Class<? extends Request> clazz) {
        return con.getRequestManager().getRequest(clazz);
    }

    public void stop() {
        running = false;
        con.end();
    }
}
