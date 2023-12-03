package com.github.redreaperlp.socketapi.communication;

import com.github.redreaperlp.socketapi.NetInstance;
import com.github.redreaperlp.socketapi.client.SocketClient;
import com.github.redreaperlp.socketapi.communication.request.Request;
import com.github.redreaperlp.socketapi.communication.request.requests.RequestPing;
import com.github.redreaperlp.socketapi.communication.request.special.RequestPromising;
import com.github.redreaperlp.socketapi.communication.response.Response;
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
                System.out.println("Waiting for " + requestQueue.size() + " requests to be sent");
                Thread.sleep(1000);
            }
            if (incomingThread != null && incomingThread.isAlive()) incomingThread.interrupt();
            if (outgoingThread != null && outgoingThread.isAlive()) outgoingThread.interrupt();
            if (timeoutThread != null && timeoutThread.isAlive()) timeoutThread.interrupt();
            if (pingThread != null && pingThread.isAlive()) pingThread.interrupt();
            if (socket.isClosed()) return;
            socket.close();
        } catch (IOException | InterruptedException e) {
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
                    if (netInstance instanceof SocketClient client) {
                        client.restart();
                    }
                    break;
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
                        if (netInstance instanceof SocketClient client) {
                            client.restart();
                        }
                        throw new RuntimeException(e);
                    }
                }
                synchronized (requestQueue) {
                    try {
                        requestQueue.wait();
                    } catch (InterruptedException e) {
                        System.out.println("Outgoing thread interrupted");
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
                        throw new RuntimeException(e);
                    }
                }
                synchronized (pendingResponses) {
                    try {
                        pendingResponses.wait();
                    } catch (InterruptedException e) {
                        System.out.println("Timeout thread interrupted");
                    }
                }
            }
        });
        timeoutThread.setName("Timeout Listener");
        timeoutThread.start();
    }

    public void ping() {
        pingThread = new Thread(this::pingLoop);
        pingThread.setName("Ping Loop");
        pingThread.start();
    }

    private void resolve(JSONObject jsonObject) {
        System.out.println("Received " + jsonObject);
        if (jsonObject.has("type")) {
            if (jsonObject.getString("type").equals("response")) {
                JSONObject response = jsonObject.getJSONObject("data");
                long id = response.getLong("id");
                RequestPromising toRem = null;
                synchronized (pendingResponses) {
                    for (RequestPromising promising : pendingResponses) {
                        if (promising.getId() == id) {
                            promising.setResponse(response);
                            promising.valudateResponse();
                            promising.finalizeAll();
                            promising.done();
                            toRem = promising;
                        }
                    }
                    pendingResponses.remove(toRem);
                }
            } else {
                String type = jsonObject.getString("type");
                if (!jsonObject.has("id")) {
                    Request r = getRequestManager().getRequest(type);
                    switch (type) {
                        case "disconnect":
                            System.out.println("Received disconnect request");
                            end();
                            break;
                    }
                } else {
                    Request s = getRequestManager().getRequest(type, jsonObject.getLong("id"));
                    if (s instanceof RequestPromising promising) {
                        if (jsonObject.has("data")) {
                            promising.setData(jsonObject.getJSONObject("data"));
                        }
                        promising.validateRequest();
                        promising.getResponse().queue();
                    }
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

    private void pingLoop() {
        try {
            while (!pingThread.isInterrupted()) {
                Thread.sleep(3000);
                requestManager.getRequest(RequestPing.class).complete();
            }
        } catch (InterruptedException e) {
            System.out.println("Ping thread interrupted");
        }
    }
}
