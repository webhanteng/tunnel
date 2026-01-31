package com.hanteng.tunnel.server.registry;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
public class RouteRegistry {

    private final Map<String, List<ClientSession>> routes = new ConcurrentHashMap<>();

    public void register(String route, ClientSession session) {
        routes.computeIfAbsent(route, k -> new CopyOnWriteArrayList<>()).add(session);
    }

    public void remove(ClientSession session) {
        routes.values().forEach(list -> list.remove(session));
    }

    public void removeByRoute(String route) {
        List<ClientSession> sessions = routes.remove(route);
        if (sessions != null) {
            sessions.forEach(ClientSession::cleanup);
        }
    }

    public void removeByClientId(String clientId) {
        routes.values().forEach(list -> {
            list.removeIf(session -> {
                if (session.getId().equals(clientId)) {
                    session.cleanup();
                    return true;
                }
                return false;
            });
        });
    }

    public Map<String, List<ClientSession>> snapshot() {
        return routes;
    }

    public ClientSession findById(String clientId) {
        for (List<ClientSession> sessions : routes.values()) {
            for (ClientSession session : sessions) {
                if (session.getId().equals(clientId)) {
                    return session;
                }
            }
        }
        return null;
    }

    public List<ClientSession> getAllSessions() {
        return routes.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public Map<String, Integer> getRouteCounts() {
        return routes.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().size()
                ));
    }
}
