package com.site.whiteboard.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.site.whiteboard.whiteboard.DrawEvent;
import com.site.whiteboard.whiteboard.DrawEvent.DrawEventType;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
    
    private final SimpMessageSendingOperations messageTemplate;

    //Informs the server when a user disconnects
    @EventListener
    public void handleWebSocketConnectDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        if (username != null) {
            log.info("User disconnected: {}", username);
            var drawEvent = DrawEvent.builder()
                .type(DrawEventType.END)
                .username(username)
                .build();
            messageTemplate.convertAndSend("/topic/whiteboard", drawEvent);
        }
    }
}
