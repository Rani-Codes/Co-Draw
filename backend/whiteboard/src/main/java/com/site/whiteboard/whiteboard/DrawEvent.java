package com.site.whiteboard.whiteboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DrawEvent {
    private String username;
    private DrawEventType type;
    private double x;
    private double y;
    private String color;
    private double lineWidth;
    private boolean isEraser;  // Flag to indicate if the tool is an eraser
    
    public enum DrawEventType {
        START,      // When user starts drawing
        DRAW,       // When user is drawing
        ERASE,       // When user is erasing
        END,        // When user ends drawing
        CLEAR      // When user clears the canvas
    }
} 