package com.site.whiteboard.whiteboard;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WhiteboardController {

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
        return drawEvent;
    }

    @MessageMapping("/whiteboard.clear")
    @SendTo("/topic/whiteboard")
    public DrawEvent handleClearEvent(DrawEvent drawEvent) {
        if (drawEvent.getType() == DrawEvent.DrawEventType.CLEAR) {
            return drawEvent;
        }
        return null;
    }
} 