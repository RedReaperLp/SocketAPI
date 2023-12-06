package com.github.redreaperlp.socketapi.communication;

import com.github.redreaperlp.socketapi.NetInstance;
import com.github.redreaperlp.socketapi.client.SocketClient;
import com.github.redreaperlp.socketapi.communication.request.Request;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestPing;
import com.github.redreaperlp.socketapi.communication.request.special.RequestPromising;
import com.github.redreaperlp.socketapi.communication.response.Response;
import com.github.redreaperlp.socketapi.server.SocketServer;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public abstract class Connection {
    private Socket socket;
    private NetInstance netInstance;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Thread incomingThread;
    private Thread outgoingThread;
    private Thread timeoutThread;
    private Thread pingThread;
    private final RequestManager requestManager;
    public final List<RequestPromising> pendingResponses = new ArrayList<>();
    private final List<Request> requestQueue = new ArrayList<>();

    public Connection(Socket socket, NetInstance netInstance) {
        this.socket = socket;
        this.netInstance = netInstance;
        requestManager = new RequestManager(netInstance);
        requestManager.setConnection(this);
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BufferedReader getReader() {
        return reader;
    }

    public BufferedWriter getWriter() {
        return writer;
    }

    public boolean isAlive() {
        return false;
    }

    public void end() {
        System.out.println(requestQueue.size() + " requests left in queue");
        try {
            if (!requestQueue.isEmpty()) {
                for (Request request : requestQueue) {
                    if (request instanceof RequestPromising promising) {
                        promising.failed(408);
                        promising.done();
                    }
                }
            }
            if (pingThread != null && pingThread.isAlive()) pingThread.interrupt();
            if (incomingThread != null && incomingThread.isAlive()) incomingThread.interrupt();
            if (outgoingThread != null && outgoingThread.isAlive()) outgoingThread.interrupt();
            if (timeoutThread != null && timeoutThread.isAlive()) timeoutThread.interrupt();
            if (socket.isClosed()) return;
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void incoming() {
        incomingThread = new Thread(() -> {
            while (!incomingThread.isInterrupted()) {
                try {
                    String line = reader.readLine();
                    if (line != null) {
                        JSONObject jsonObject = new JSONObject(line);
                        resolve(jsonObject);
                    }
                } catch (IOException e) {
                    connectionError();
                    System.out.println("Incoming thread interrupted");
                    return;
                }
            }
        });
        incomingThread.setName("Incoming Listener");
        incomingThread.start();
    }

    /**
     * Sends all requests in the queue
     */
    public void outgoing() {
        outgoingThread = new Thread(() -> {
            while (!outgoingThread.isInterrupted()) {
                while (!requestQueue.isEmpty()) {
                    Request request = requestQueue.remove(0);
                    if (request == null) continue;
                    JSONObject jsonObject = new JSONObject();
                    if (request instanceof RequestPromising promising) {
                        if (!promising.isResponding()) {
                            jsonObject.put("type", request.getType());
                            jsonObject.put("data", request.getData());
                        } else {
                            Response response = promising.getResponse();
                            response.pack();
                            jsonObject.put("type", "response");
                            jsonObject.put("data", response.getData());
                        }
                        jsonObject.put("id", promising.getId());
                    } else {
                        jsonObject.put("type", request.getType());
                        jsonObject.put("data", request.getData());
                    }
                    try {
                        System.out.println("Sending " + jsonObject);
                        writer.write(jsonObject.toString());
                        writer.newLine();
                        writer.flush();
                    } catch (IOException e) {
                        connectionError();
                        System.out.println("Outgoing thread interrupted");
                        return;
                    }
                }
                synchronized (requestQueue) {
                    try {
                        requestQueue.wait();
                    } catch (InterruptedException e) {
                        System.out.println("Outgoing thread interrupted");
                        return;
                    }
                }
            }
        });
        outgoingThread.setName("Outgoing Listener");
        outgoingThread.start();
    }

    public void timeout() {
        timeoutThread = new Thread(() -> {
            while (!timeoutThread.isInterrupted()) {
                while (!pendingResponses.isEmpty()) {
                    synchronized (pendingResponses) {
                        List<RequestPromising> toRemove = new ArrayList<>();
                        for (RequestPromising promising : pendingResponses) {
                            if (System.currentTimeMillis() - promising.getTimeSent() > 5000) {
                                System.out.println("Request " + promising.getType() + " timed out");
                                promising.failed(408);
                                promising.done();
                                toRemove.add(promising);
                            }
                        }
                        pendingResponses.removeAll(toRemove);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("Timeout thread interrupted");
                        return;
                    }
                }
                synchronized (pendingResponses) {
                    try {
                        pendingResponses.wait();
                    } catch (InterruptedException e) {
                        System.out.println("Timeout thread interrupted");
                        return;
                    }
                }
            }
        });
        timeoutThread.setName("Timeout Listener");
        timeoutThread.start();
    }

    public void connectionError() {
        if (netInstance instanceof SocketClient client) {
            synchronized (client.getConnectionError()) {
                client.getConnectionError().notifyAll();
            }
        }
    }

    public void ping() {
        pingThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    System.out.println("Ping thread interrupted");
                    return;
                }
                getRequestManager().getRequest(RequestPing.class).complete();
            }
        });
        pingThread.setName("Ping Thread");
        pingThread.start();
    }

    private void resolve(JSONObject jsonObject) {
        if (jsonObject.has("type")) {
            if (jsonObject.getString("type").equals("response")) {
                JSONObject data = jsonObject.getJSONObject("data");
                long id = data.getLong("id");
                RequestPromising toRem = null;
                synchronized (pendingResponses) {
                    for (RequestPromising promising : pendingResponses) {
                        if (promising.getId() == id) {
                            netInstance.getRequestHandler().handleRequest(promising, data);
                            promising.setResponse(data == null ? new JSONObject() : data);
                            promising.validateResponse();
                            promising.done();
                            toRem = promising;
                        }
                    }
                    pendingResponses.remove(toRem);
                }
            } else {
                String type = jsonObject.getString("type");
                Request s = getRequestManager().getRequest(type, jsonObject.getLong("id"));
                if (s instanceof RequestPromising promising) {
                    netInstance.getRequestHandler().handleRequest(promising, jsonObject.has("data")? jsonObject.getJSONObject("data") : new JSONObject());
                    promising.validateRequest();
                    promising.getResponse().queue();
                }
            }
        } else {
            System.out.println("Received invalid request " + jsonObject);
        }
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public void queue(Request request) {
        if (request == null) return;

        synchronized (requestQueue) {
            requestQueue.add(request);
            requestQueue.notifyAll();
        }

        if (request instanceof RequestPromising promising) {
            synchronized (pendingResponses) {
                pendingResponses.add(promising);
                pendingResponses.notifyAll();
            }
        }
    }


    public void queuePriority(Request request) {
        if (request instanceof RequestPromising promising) {
            synchronized (pendingResponses) {
                pendingResponses.notifyAll();
                pendingResponses.add(promising);
            }
        }
        synchronized (requestQueue) {
            requestQueue.add(0, request);
            requestQueue.notifyAll();
        }
    }
    
    public Socket getSocket() {
        return socket;
    }
}
