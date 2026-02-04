package com.hanteng.tunnel.client.ws;

import com.hanteng.tunnel.client.protocol.HeartbeatMessage;
import com.hanteng.tunnel.client.protocol.TunnelRequest;
import com.hanteng.tunnel.client.protocol.TunnelResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class TunnelClient extends AbstractWebSocketHandler {

    @Value("${tunnel.server-ws}")
    String wsUrl;

    @Value("${tunnel.local-backend}")
    String backend;

    private WebSocketSession session;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String getTimestamp() {
        return LocalDateTime.now().format(formatter);
    }

    @PostConstruct
    public void connect() {
        new Thread(() -> {
            int attempt = 1;
            while (true) {
                try {
                    System.out.println("[" + getTimestamp() + "] Attempting to connect to tunnel server... (attempt " + attempt + ")");
                    System.out.println("[" + getTimestamp() + "] WebSocket URL: " + wsUrl);
                    
                    StandardWebSocketClient client = new StandardWebSocketClient();
                    session = client.doHandshake(this, wsUrl).get();
                    
                    System.out.println("[" + getTimestamp() + "] ✅ Connected to tunnel server!");
                    System.out.println("[" + getTimestamp() + "] Session ID: " + session.getId());
                    break;
                } catch (Exception e) {
                    System.err.println("[" + getTimestamp() + "] ❌ Failed to connect: " + e.getMessage());
                    System.err.println("[" + getTimestamp() + "] Retrying in 3 seconds...");
                    try {
                        TimeUnit.SECONDS.sleep(3);
                        attempt++;
                    } catch (InterruptedException ex) {
                        System.err.println("[" + getTimestamp() + "] ❌ Connection thread interrupted");
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }).start();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        if (HeartbeatMessage.PONG.equals(payload)) {
            System.out.println("[" + getTimestamp() + "] 🏓 Received PONG from server");
            return;
        }

        System.out.println("[" + getTimestamp() + "] 📥 Received message from server");

        try {
            TunnelRequest request = objectMapper.readValue(payload, TunnelRequest.class);
            System.out.println("[" + getTimestamp() + "] 📋 Request ID: " + request.getRequestId());
            System.out.println("[" + getTimestamp() + "] 📋 Method: " + request.getMethod());
            System.out.println("[" + getTimestamp() + "] 📋 Path: " + request.getPath());
            System.out.println("[" + getTimestamp() + "] 📋 Forwarding to local backend: " + backend);

            TunnelResponse response = handleRequest(request);
            System.out.println("[" + getTimestamp() + "] 📥 Received response from local backend");
            System.out.println("[" + getTimestamp() + "] 📋 Status: " + response.getStatus());

            sendResponse(session, response);
        } catch (Exception e) {
            System.err.println("[" + getTimestamp() + "] ❌ Failed to handle request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        try {
            byte[] payload = message.getPayload().array();
            TunnelRequest request = objectMapper.readValue(payload, TunnelRequest.class);
            System.out.println("[" + getTimestamp() + "] 📥 Received binary message from server");
            System.out.println("[" + getTimestamp() + "] 📋 Request ID: " + request.getRequestId());
            System.out.println("[" + getTimestamp() + "] 📋 Method: " + request.getMethod());
            System.out.println("[" + getTimestamp() + "] 📋 Path: " + request.getPath());
            System.out.println("[" + getTimestamp() + "] 📋 Forwarding to local backend: " + backend);

            TunnelResponse response = handleRequest(request);
            System.out.println("[" + getTimestamp() + "] 📥 Received response from local backend");
            System.out.println("[" + getTimestamp() + "] 📋 Status: " + response.getStatus());

            sendResponse(session, response);
        } catch (Exception e) {
            System.err.println("[" + getTimestamp() + "] ❌ Failed to handle binary request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendResponse(WebSocketSession session, TunnelResponse response) throws Exception {
        byte[] responseBytes = objectMapper.writeValueAsBytes(response);
        if (responseBytes.length > 1024 * 1024) { // 超过 1MB 使用二进制消息
            System.out.println("[" + getTimestamp() + "] 📤 Sending large response as binary message: " + responseBytes.length + " bytes");
            session.sendMessage(new BinaryMessage(responseBytes));
        } else {
            System.out.println("[" + getTimestamp() + "] 📤 Sending small response as text message: " + responseBytes.length + " bytes");
            session.sendMessage(new TextMessage(responseBytes));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("[" + getTimestamp() + "] ❌ Connection closed: " + status.getReason());
        System.out.println("[" + getTimestamp() + "] Reconnecting...");
        connect();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.err.println("[" + getTimestamp() + "] ❌ Transport error: " + exception.getMessage());
    }

    private TunnelResponse handleRequest(TunnelRequest request) {
        TunnelResponse response = new TunnelResponse();
        response.setRequestId(request.getRequestId());

        try {
            String url = backend + request.getPath();
            System.out.println("[" + getTimestamp() + "] 🚀 Calling local backend: " + url);

            HttpHeaders headers = new HttpHeaders();
            if (request.getHeaders() != null) {
                System.out.println("[" + getTimestamp() + "] 📋 Headers:");
                for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                    headers.add(entry.getKey(), entry.getValue());
                    System.out.println("[" + getTimestamp() + "]   - " + entry.getKey() + ": " + entry.getValue());
                }
            }

            HttpEntity<byte[]> entity = new HttpEntity<>(request.getBody(), headers);
            if (request.getBody() != null) {
                System.out.println("[" + getTimestamp() + "] 📋 Body length: " + request.getBody().length + " bytes");
            }

            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            ResponseEntity<byte[]> httpResponse = restTemplate.exchange(url, method, entity, byte[].class);
            System.out.println("[" + getTimestamp() + "] ✅ Backend response status: " + httpResponse.getStatusCode());

            response.setStatus(httpResponse.getStatusCodeValue());
            response.setBody(httpResponse.getBody());

            if (httpResponse.getHeaders() != null) {
                Map<String, String> responseHeaders = new java.util.HashMap<>();
                httpResponse.getHeaders().forEach((key, values) -> {
                    if (values != null && !values.isEmpty()) {
                        responseHeaders.put(key, values.get(0));
                    }
                });
                response.setHeaders(responseHeaders);
                System.out.println("[" + getTimestamp() + "] 📋 Response headers: " + responseHeaders.keySet());
            }

        } catch (Exception e) {
            System.err.println("[" + getTimestamp() + "] ❌ Failed to call local backend: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(500);
            response.setBody(("Error: " + e.getMessage()).getBytes());
        }

        return response;
    }

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(HeartbeatMessage.PING));
                System.out.println("[" + getTimestamp() + "] 🏓 Sent PING to server");
            } catch (Exception e) {
                System.err.println("[" + getTimestamp() + "] ❌ Failed to send heartbeat: " + e.getMessage());
            }
        } else {
            System.out.println("[" + getTimestamp() + "] ⚠️  Cannot send heartbeat - session not connected");
        }
    }
}
