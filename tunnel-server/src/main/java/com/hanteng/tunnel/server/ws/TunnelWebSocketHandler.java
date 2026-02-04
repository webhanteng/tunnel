package com.hanteng.tunnel.server.ws;

import com.hanteng.tunnel.server.protocol.HeartbeatMessage;
import com.hanteng.tunnel.server.protocol.TunnelResponse;
import com.hanteng.tunnel.server.registry.ClientSession;
import com.hanteng.tunnel.server.registry.RouteRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

@Component
public class TunnelWebSocketHandler extends AbstractWebSocketHandler {

    @Autowired
    private RouteRegistry registry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("route=")) {
            String route = query.split("route=")[1].split("&")[0];
            registry.register(route, new ClientSession(route, session));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        if (HeartbeatMessage.PING.equals(payload)) {
            session.sendMessage(new TextMessage(HeartbeatMessage.PONG));
            ClientSession clientSession = findClientSession(session);
            if (clientSession != null) {
                clientSession.updateHeartbeat();
            }
            return;
        }

        try {
            TunnelResponse response = objectMapper.readValue(payload, TunnelResponse.class);
            ClientSession clientSession = findClientSession(session);
            if (clientSession != null) {
                clientSession.handleResponse(response);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse message: " + e.getMessage());
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        try {
            byte[] payload = message.getPayload().array();
            TunnelResponse response = objectMapper.readValue(payload, TunnelResponse.class);
            ClientSession clientSession = findClientSession(session);
            if (clientSession != null) {
                clientSession.handleResponse(response);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse binary message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ClientSession clientSession = new ClientSession(null, session);
        clientSession.cleanup();
        registry.remove(clientSession);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        ClientSession clientSession = new ClientSession(null, session);
        clientSession.cleanup();
        registry.remove(clientSession);
    }

    private ClientSession findClientSession(WebSocketSession session) {
        for (ClientSession clientSession : registry.getAllSessions()) {
            if (clientSession.getSession().getId().equals(session.getId())) {
                return clientSession;
            }
        }
        return null;
    }
}
