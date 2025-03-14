package com.site.whiteboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

//This class is used to configure the websocket connection and the message broker
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") //endpoint for websocket connections
                .setAllowedOriginPatterns("http://localhost:[*]") //allow all origins
                .withSockJS(); //enable fallback options for browsers that don't support websocket
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue", "/user"); //enable a simple memory-based message broker to carry the messages to the client
        config.setApplicationDestinationPrefixes("/app"); //prefix for endpoints the client can send messages to
        config.setUserDestinationPrefix("/user"); // prefix for user-specific messages
    }
}


