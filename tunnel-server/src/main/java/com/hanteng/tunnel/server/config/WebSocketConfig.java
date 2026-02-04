package com.hanteng.tunnel.server.config;

import com.hanteng.tunnel.server.ws.TunnelWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private TunnelWebSocketHandler tunnelWebSocketHandler;

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 增加消息大小限制到 10MB
        container.setMaxTextMessageBufferSize(1024 * 1024 * 10);
        container.setMaxBinaryMessageBufferSize(1024 * 1024 * 10);
        return container;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(tunnelWebSocketHandler, "/ws")
                .setAllowedOrigins("*"); // 内网环境直接放开
    }
}
