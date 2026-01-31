package com.hanteng.tunnel.server.lb;

import com.hanteng.tunnel.server.registry.ClientSession;
import com.hanteng.tunnel.server.registry.RouteRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinLoadBalancer {

    private final ConcurrentHashMap<String, AtomicInteger> indexMap = new ConcurrentHashMap<>();

    public ClientSession select(String route, RouteRegistry registry) {
        List<ClientSession> clients = registry.snapshot().get(route);
        if (clients == null || clients.isEmpty()) {
            return null;
        }

        AtomicInteger idx = indexMap.computeIfAbsent(route, r -> new AtomicInteger(0));
        int i = Math.abs(idx.getAndIncrement() % clients.size());
        return clients.get(i);
    }
}
