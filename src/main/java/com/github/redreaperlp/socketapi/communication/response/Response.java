package com.github.redreaperlp.socketapi.communication.response;

import com.github.redreaperlp.socketapi.communication.request.special.RequestVoiding;
import org.json.JSONObject;

public class Response extends RequestVoiding {
    private int status;
    private JSONObject data;
    private long id;

    /**
     * Creates a Response from a status and a JSONObject
     *
     * @param status The status of the response
     * @param data   The data of the response
     */
    public Response(int status, JSONObject data) {
        this.status = status;
        this.data = data;
    }

    public Response() {
    }

    /**
     * Creates a Response from a JSONObject that was received
     *
     * @param data The JSONObject that was received
     */
    public Response(JSONObject data) {
        status = data.getInt("status");
        this.data = data.getJSONObject("data");
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String getName() {
        return "response";
    }

    public JSONObject getData() {
        return data;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }

    @Override
    public void pack() {
        if (data == null) setData(new JSONObject());
        setData(data.put("status", status).put("id", id));
    }

    public void setID(long id) {
        this.id = id;
    }

    public long getID() {
        return id;
    }
}
