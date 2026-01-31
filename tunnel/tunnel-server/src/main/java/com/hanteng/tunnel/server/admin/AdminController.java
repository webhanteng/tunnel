package com.hanteng.tunnel.server.admin;

import com.hanteng.tunnel.server.registry.ClientSession;
import com.hanteng.tunnel.server.registry.RouteRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    RouteRegistry registry;

    @GetMapping("/routes")
    public Map<String, Integer> routes() {
        return registry.getRouteCounts();
    }

    @GetMapping("/clients")
    public List<Map<String, Object>> clients() {
        return registry.getAllSessions().stream()
                .map(this::toClientInfo)
                .collect(java.util.stream.Collectors.toList());
    }

    @DeleteMapping("/routes/{route}")
    public ResponseEntity<String> disconnectRoute(@PathVariable String route) {
        registry.removeByRoute(route);
        return ResponseEntity.ok("Route " + route + " disconnected");
    }

    @DeleteMapping("/clients/{clientId}")
    public ResponseEntity<String> disconnectClient(@PathVariable String clientId) {
        registry.removeByClientId(clientId);
        return ResponseEntity.ok("Client " + clientId + " disconnected");
    }

    private Map<String, Object> toClientInfo(ClientSession session) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", session.getId());
        info.put("route", session.getRoute());
        info.put("lastHeartbeat", session.getLastHeartbeatTime());
        return info;
    }
}
