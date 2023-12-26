package com.github.redreaperlp.socketapi.communication.request.special;

import com.github.redreaperlp.socketapi.communication.RequestManager;
import com.github.redreaperlp.socketapi.communication.request.Request;
import com.github.redreaperlp.socketapi.communication.response.Response;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class RequestPromising implements Request {
    private RequestManager manager;
    private Response response;
    private JSONObject data;
    private long id;
    private long timeSent;
    private long timeReceived;
    private final Object lock = new Object();
    private int failed = 200;
    private boolean isResponding = false;
    private AtomicBoolean responseReceived = new AtomicBoolean(false);

    public RequestPromising(long id) {
        this.id = id;
    }

    /**
     * Sends the request and waits for the response
     * @return
     */
    public Response complete() {
        queue();
        synchronized (lock) {
            try {
                System.out.print("Sent ");
                if (!responseReceived.get()) {
                    lock.wait();
                }
                timeReceived = System.currentTimeMillis();
                System.out.println("-> " + getName() + " responded after " + (System.currentTimeMillis() - getTimeSent()) + "ms (id: " + getId() + ")");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return getResponse();
    }

    /**
     * Queues the request but does not wait for the response
     */
    @Override
    public void queue() {
        setTimeSent(System.currentTimeMillis());
        Request.super.queue();
    }

    /**
     * @return The request type
     */
    @Override
    public RequestManager getManager() {
        return manager;
    }

    @Override
    public void setManager(RequestManager manager) {
        this.manager = manager;
    }

    /**
     * Sets the data of the request
     * @param data The data of the request
     */
    @Override
    public void setData(JSONObject data) {
        this.data = data;
    }

    @Override
    public JSONObject getData() {
        return data;
    }

    /**
     * Packs the request into a JSONObject, this will be called before sending the request
     *
     * @apiNote Used if there are additional fields in the requests class
     */
    @Override
    public void pack() {}

    /**
     * @return The response of the request
     */
    public Response getResponse() {
        if (response == null) {
            response = getManager().getRequest(Response.class, getId());
            response.setID(id);
        }
        return response;
    }

    /**
     * Sets the response data and status
     *
     * @param data   The response data
     * @param status The response status
     */
    public void setResponse(JSONObject data, int status) {
        isResponding = true;
        if (response == null) {
            response = getResponse();
        }
        response.setData(data);
        response.setStatus(status);
        responseReceived.set(true);
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    /**
     * Sets the response data and status
     *
     * @param data The response data
     * @apiNote This will set the status to the status from the data
     */
    public void setResponse(JSONObject data) {
        if (data.has("status")) {
            setResponse(data, data.getInt("status"));
        } else {
            setResponse(data);
        }
    }

    /**
     * @return The request id
     */
    public long getId() {
        return id;
    }

    /**
     * Notifies the waiting thread that the request is done
     *
     * @apiNote This will be called automatically when the response is received and handled
     */
    public void done() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    /**
     * This returns the time when the request was sent in milliseconds
     * @return The time the request was sent
     */
    public long getTimeSent() {
        return timeSent;
    }

    /**
     * This returns the time span between sending and receiving the request in milliseconds
     * @return The time span between sending and receiving the request
     */
    public long getLatency() {
        return timeReceived - timeSent;
    }

    /**
     * Sets the time when the request was sent
     * @param timeSent The time when the request was sent
     */
    public void setTimeSent(long timeSent) {
        this.timeSent = timeSent;
    }

    /**
     * Sets the failed status
     *
     * @param status The status code
     */
    public void failed(int status) {
        failed = status;
    }

    /**
     * Returns the failed status code
     * @return The failed status code
     * @apiNote This will be 200 if the request was successful
     */
    public int failed() {
        return failed;
    }

    /**
     * Validates the request, if the request is not valid, it should call {@link #failed(int)}
     *
     * @apiNote This should check for possible null values etc.
     */
    public void validateRequest(){};

    /**
     * Validates the response, if the response is not valid, it should call {@link #failed(int)}
     */
    public void validateResponse(){};

    public boolean isResponding() {
        return isResponding;
    }
}


//Todo: For preventing a response to arrive before the wait is called add a new Thread that checks if a request has been responded, otherwise the program hangs up