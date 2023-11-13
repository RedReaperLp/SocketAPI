package com.github.redreaperlp.socketapi.communication.request.requests;

import com.github.redreaperlp.socketapi.communication.request.special.RequestPromising;
import com.github.redreaperlp.socketapi.server.Server;
import org.json.JSONObject;

public class RequestRegister extends RequestPromising {
    private final String type = "register";
    private String connectionIdentifier;

    public RequestRegister(long id) {
        super(id);
    }

    @Override
    public String getType() {
        return type;
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
        Server server = (Server) getManager().getNetInstance();
        if (getData() == null) {
            setResponse(new JSONObject().put("success", false).put("reason", "no data"), 400);
            return;
        }
        if (!server.getRegisteredConnectionClasses().isEmpty()) {
            if (!server.hasIdentifier(getData().get("identifier").toString())) {
                setResponse(new JSONObject().put("success", false).put("reason", "identifier not found"), 404);
            }
        }
        setResponse(new JSONObject().put("success", true), 200);
    }

    @Override
    public void valudateResponse() {
        if (getResponse().getStatus() != 200) {
            failed(getResponse().getStatus());
        }
    }
}
