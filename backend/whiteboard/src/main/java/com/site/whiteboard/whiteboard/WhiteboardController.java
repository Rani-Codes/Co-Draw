package com.site.whiteboard.whiteboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WhiteboardController {

    private final SimpMessagingTemplate messagingTemplate;
    
    // Store drawing events to replay for new users
    private final List<DrawEvent> drawHistory = new ArrayList<>();

    //User goes to /app/whiteboard.draw to send a draw event
    @MessageMapping("/whiteboard.draw")
    @SendTo("/topic/whiteboard")
    public DrawEvent handleDrawEvent(DrawEvent drawEvent) {
        // Handle both drawing and erasing events
        if (drawEvent.getType() == DrawEvent.DrawEventType.ERASE) {
            drawEvent.setEraser(true);
            // You might want to set a specific color or width for the eraser
            drawEvent.setColor("#ffffff"); // Set to white or your canvas background color
        }
        
        // Store the event in history
        drawHistory.add(drawEvent);
        
        return drawEvent;
    }

    //User goes to /app/whiteboard.clear to send a clear event to clear the canvas
    @MessageMapping("/whiteboard.clear")
    @SendTo("/topic/whiteboard")
    public DrawEvent handleClearEvent(DrawEvent drawEvent) {
        if (drawEvent.getType() == DrawEvent.DrawEventType.CLEAR) {
            // Clear the history when canvas is cleared
            drawHistory.clear();
            return drawEvent;
        }
        return null;
    }
    
    // Endpoint for new users to get the current canvas state
    @MessageMapping("/whiteboard.join")
    public void handleJoinEvent(DrawEvent joinEvent) {
        // Create a container for history batch
        Map<String, Object> historyBatch = new HashMap<>();
        historyBatch.put("type", "HISTORY_BATCH");
        historyBatch.put("events", drawHistory);
        
        // Send the entire drawing history to the new user as a single batch
        messagingTemplate.convertAndSendToUser(
            joinEvent.getUsername(), 
            "/queue/whiteboard.history.batch", 
            historyBatch
        );
    }
    
} 