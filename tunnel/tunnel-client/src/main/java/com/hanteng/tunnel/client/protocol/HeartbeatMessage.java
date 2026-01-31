package com.hanteng.tunnel.client.protocol;

public class HeartbeatMessage {
    public static final String PING = "PING";
    public static final String PONG = "PONG";

    private String type;

    public HeartbeatMessage() {
    }

    public HeartbeatMessage(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
