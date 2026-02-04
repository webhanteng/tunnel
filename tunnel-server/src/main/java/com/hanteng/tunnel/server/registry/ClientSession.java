package com.hanteng.tunnel.server.registry;

import com.hanteng.tunnel.server.protocol.TunnelRequest;
import com.hanteng.tunnel.server.protocol.TunnelResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ClientSession {

    private final String id = UUID.randomUUID().toString();
    private final String route;
    private final WebSocketSession session;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, CompletableFuture<ResponseEntity<byte[]>>> pendingRequests = new ConcurrentHashMap<>();

    private volatile long lastHeartbeatTime = System.currentTimeMillis();

    public ClientSession(String route, WebSocketSession session) {
        this.route = route;
        this.session = session;
    }

    public String getId() {
        return id;
    }

    public String getRoute() {
        return route;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public long getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void updateHeartbeat() {
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    public ResponseEntity<byte[]> forward(String method, String path, HttpHeaders headers, byte[] body) throws Exception {
        String requestId = UUID.randomUUID().toString();

        TunnelRequest tunnelRequest = new TunnelRequest();
        tunnelRequest.setRequestId(requestId);
        tunnelRequest.setMethod(method);
        tunnelRequest.setPath(path);

        if (headers != null) {
            Map<String, String> headerMap = new ConcurrentHashMap<>();
            headers.forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    headerMap.put(key, values.get(0));
                }
            });
            tunnelRequest.setHeaders(headerMap);
        }

        tunnelRequest.setBody(body);

        CompletableFuture<ResponseEntity<byte[]>> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        try {
            byte[] requestBytes = objectMapper.writeValueAsBytes(tunnelRequest);
            if (requestBytes.length > 1024 * 1024) { // 超过 1MB 使用二进制消息
                System.out.println("Sending large request as binary message: " + requestBytes.length + " bytes");
                session.sendMessage(new BinaryMessage(requestBytes));
            } else {
                System.out.println("Sending small request as text message: " + requestBytes.length + " bytes");
                session.sendMessage(new TextMessage(requestBytes));
            }

            return future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            pendingRequests.remove(requestId);
            throw e;
        }
    }

    public void handleResponse(TunnelResponse response) {
        CompletableFuture<ResponseEntity<byte[]>> future = pendingRequests.remove(response.getRequestId());
        if (future != null) {
            HttpHeaders headers = new HttpHeaders();
            if (response.getHeaders() != null) {
                for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
                    headers.add(entry.getKey(), entry.getValue());
                }
            }

            ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(
                    response.getBody(),
                    headers,
                    response.getStatus()
            );

            future.complete(responseEntity);
        }
    }

    public void handleError(String requestId, Exception e) {
        CompletableFuture<ResponseEntity<byte[]>> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.completeExceptionally(e);
        }
    }

    public void cleanup() {
        for (CompletableFuture<ResponseEntity<byte[]>> future : pendingRequests.values()) {
            future.completeExceptionally(new Exception("Client disconnected"));
        }
        pendingRequests.clear();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ClientSession &&
                ((ClientSession) o).session.getId().equals(session.getId());
    }

    @Override
    public int hashCode() {
        return session.getId().hashCode();
    }
}
