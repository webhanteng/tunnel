package com.hanteng.tunnel.server.scheduler;

import com.hanteng.tunnel.server.registry.ClientSession;
import com.hanteng.tunnel.server.registry.RouteRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HeartbeatChecker {

    @Autowired
    private RouteRegistry registry;

    private static final long HEARTBEAT_TIMEOUT = 60000;

    @Scheduled(fixedRate = 10000)
    public void checkHeartbeat() {
        List<ClientSession> allSessions = registry.getAllSessions();
        long currentTime = System.currentTimeMillis();

        for (ClientSession session : allSessions) {
            if (currentTime - session.getLastHeartbeatTime() > HEARTBEAT_TIMEOUT) {
                try {
                    session.getSession().close();
                    registry.remove(session);
                    System.out.println("Client " + session.getId() + " disconnected due to heartbeat timeout");
                } catch (Exception e) {
                    System.err.println("Failed to close session: " + e.getMessage());
                }
            }
        }
    }
}
