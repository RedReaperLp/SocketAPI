package com.github.redreaperlp.socketapi.communication.request.requests;

import com.github.redreaperlp.socketapi.communication.request.special.RequestPromising;
import org.json.JSONObject;

public class RequestRegister extends RequestPromising {
    public static final String name = "register";
    private String connectionIdentifier;

    public RequestRegister(long id) {
        super(id);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setConnectionIdentifier(String connectionIdentifier) {
        this.connectionIdentifier = connectionIdentifier;
    }

    @Override
    public void pack() {
        super.pack();
        setData(new JSONObject().put("identifier", connectionIdentifier));
    }

    @Override
    public void validateRequest() {
    }

    @Override
    public void validateResponse() {
        if (getResponse().getStatus() != 200) {
            failed(getResponse().getStatus());
        }
    }
}
