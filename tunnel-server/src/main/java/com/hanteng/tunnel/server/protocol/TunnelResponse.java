package com.hanteng.tunnel.server.protocol;

import java.util.HashMap;
import java.util.Map;

public class TunnelResponse {
    private String requestId;
    private int status;
    private Map<String, String> headers;
    private byte[] body;

    public TunnelResponse() {
        this.headers = new HashMap<>();
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
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
