package com.hanteng.tunnel.server.protocol;

import java.util.HashMap;
import java.util.Map;

public class TunnelRequest {
    private String requestId;
    private String method;
    private String path;
    private Map<String, String> headers;
    private byte[] body;

    public TunnelRequest() {
        this.headers = new HashMap<>();
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}
