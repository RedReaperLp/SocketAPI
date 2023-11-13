package com.github.redreaperlp.socketapi.communication.request.special;

import com.github.redreaperlp.socketapi.communication.RequestManager;
import com.github.redreaperlp.socketapi.communication.request.Request;
import com.github.redreaperlp.socketapi.communication.response.Response;
import org.json.JSONObject;

public abstract class RequestPromising implements Request {
    private RequestManager manager;
    private Response response;
    private JSONObject data;
    private long id;
    private long timeSent;
    private final Object lock = new Object();
    private int failed = 200;
    private boolean isResponding = false;

    public RequestPromising(long id) {
        this.id = id;
    }

    public Response complete() {
        queue();
        synchronized (lock) {
            try {
                lock.wait();
                System.out.println("Request " + getType() + " responded after " + (System.currentTimeMillis() - getTimeSent()) + "ms (id: " + getId() + ")");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return getResponse();
    }

    @Override
    public void queue() {
        setTimeSent(System.currentTimeMillis());
        Request.super.queue();
    }

    @Override
    public RequestManager getManager() {
        return manager;
    }

    @Override
    public void setManager(RequestManager manager) {
        this.manager = manager;
    }

    @Override
    public void setData(JSONObject data) {
        this.data = data;
    }

    @Override
    public JSONObject getData() {
        return data;
    }

    @Override
    public void pack() {

    }

    public void finalizeAll() {
    }
    public Response getResponse() {
        if (response == null) {
            response = getManager().getRequest(Response.class, getId());
            response.setID(id);
        }
        return response;
    }

    public void setResponse(JSONObject data, int status) {
        isResponding = true;
        if (response == null) {
            response = getResponse();
        }
        response.setData(data);
        response.setStatus(status);
    }

    public void setResponse(JSONObject data) {
        setResponse(data, data.getInt("status"));
    }

    public long getId() {
        return id;
    }

    public void done() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public long getTimeSent() {
        return timeSent;
    }

    public void setTimeSent(long timeSent) {
        this.timeSent = timeSent;
    }

    public void failed(int status) {
        failed = status;
    }

    public int failed() {
        return failed;
    }

    public abstract void validateRequest();
    public abstract void valudateResponse();

    public boolean isResponding() {
        return isResponding;
    }
}
