<!-- @tailwind:ignore -->
<script lang="ts">
    /* @tailwind:ignore */
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { sendDrawEvent, type DrawEvent } from '../services/websocket';
  
    export let username: string;
    export let receivedEvents: DrawEvent[] = [];
  
    const dispatch = createEventDispatcher();
    
    let canvas: HTMLCanvasElement;
    let ctx: CanvasRenderingContext2D;
    let isDrawing = false;
    let color = '#000000';
    let lineWidth = 2;
    let isEraser = false;
    
    // Canvas state
    let paths: Map<string, {
      events: DrawEvent[],
      color: string,
      lineWidth: number,
      isEraser: boolean
    }> = new Map();
    
    // Flag to track if we're currently processing a history batch
    let isProcessingHistoryBatch = false;
    
    // Export a method to process history batch that can be called from outside
    export function processHistoryBatch(historyEvents: DrawEvent[]) {
      if (!historyEvents || historyEvents.length === 0) {
        console.warn("Received empty history batch");
        return;
      }
      
      isProcessingHistoryBatch = true;
      
      if (ctx) {
        processInitialHistory(historyEvents);
        isProcessingHistoryBatch = false;
      } else {
        // If canvas is not ready yet, wait for it
        const checkCanvasInterval = setInterval(() => {
          if (ctx) {
            clearInterval(checkCanvasInterval);
            processInitialHistory(historyEvents);
            isProcessingHistoryBatch = false;
          }
        }, 100);
        
        // Safety timeout after 5 seconds
        setTimeout(() => {
          clearInterval(checkCanvasInterval);
          isProcessingHistoryBatch = false;
        }, 5000);
      }
    }
    
    // Convert absolute coordinates to relative (percentage)
    function toRelativeCoordinates(x: number, y: number): { x: number, y: number } {
      if (!canvas) return { x, y };
      return {
        x: x / canvas.width,
        y: y / canvas.height
      };
    }
    
    // Convert relative coordinates to absolute pixels
    function toAbsoluteCoordinates(x: number, y: number): { x: number, y: number } {
      if (!canvas) return { x, y };
      return {
        x: x * canvas.width,
        y: y * canvas.height
      };
    }
    
    onMount(() => {
      if (!canvas) return;
      
      ctx = canvas.getContext('2d')!;
      
      // Set canvas size to match parent container
      const resizeCanvas = () => {
        const container = canvas.parentElement;
        if (container) {
          const oldWidth = canvas.width;
          const oldHeight = canvas.height;
          
          canvas.width = container.clientWidth;
          canvas.height = container.clientHeight;
          
          // Only redraw if size actually changed
          if (oldWidth !== canvas.width || oldHeight !== canvas.height) {
            redrawCanvas();
          }
        }
      };
      
      resizeCanvas();
      window.addEventListener('resize', resizeCanvas);
      
      return () => {
        window.removeEventListener('resize', resizeCanvas);
      };
    });
    /* @tailwind:end-ignore */
    
    // Watch for received events from other users or history
    $: {
      if (receivedEvents.length > 0 && ctx && !isProcessingHistoryBatch) {
        // Process just the latest event - assume history batch is already handled
        const latestEvent = receivedEvents[receivedEvents.length - 1];
        handleRemoteDrawEvent(latestEvent);
      }
    }
    
    // Process all initial history events at once for efficiency
    function processInitialHistory(events: DrawEvent[]) {
      console.log(`Processing initial history with ${events.length} events`);
      
      if (!ctx || !canvas) {
        console.error("Canvas context not available yet");
        return;
      }
      
      // Clear any existing paths and canvas
      clearCanvas();
      paths.clear();
      
      // First, process any CLEAR events to get the latest canvas state
      let lastClearIndex = -1;
      for (let i = 0; i < events.length; i++) {
        if (events[i].type === 'CLEAR') {
          lastClearIndex = i;
        }
      }
      
      // If we found a CLEAR event, only process events after it
      const eventsToProcess = lastClearIndex >= 0 ? events.slice(lastClearIndex + 1) : events;
      console.log(`Processing ${eventsToProcess.length} events after filtering CLEAR events`);
      
      // Group events by user and drawing session
      // A drawing session starts with START and ends with END
      const drawingSessions: DrawEvent[][] = [];
      let currentSession: DrawEvent[] = [];
      
      // First, group events into proper drawing sessions
      for (let i = 0; i < eventsToProcess.length; i++) {
        const event = eventsToProcess[i];
        
        // Skip our own events
        if (event.username === username) continue;
        
        if (event.type === 'START') {
          // Start a new session
          if (currentSession.length > 0) {
            // If we have an ongoing session without an END, close it and start a new one
            drawingSessions.push([...currentSession]);
            currentSession = [];
          }
          currentSession.push(event);
        } else if (event.type === 'DRAW' || event.type === 'ERASE') {
          // Add to current session if it exists
          if (currentSession.length > 0) {
            currentSession.push(event);
          } else {
            // If we get a DRAW without a START, create a synthetic session
            const syntheticStart: DrawEvent = {
              ...event,
              type: 'START'
            };
            currentSession = [syntheticStart, event];
          }
        } else if (event.type === 'END') {
          // End the current session if it exists
          if (currentSession.length > 0) {
            currentSession.push(event);
            drawingSessions.push([...currentSession]);
            currentSession = [];
          }
        }
      }
      
      // Add any remaining session
      if (currentSession.length > 0) {
        drawingSessions.push([...currentSession]);
      }
      
      console.log(`Grouped events into ${drawingSessions.length} drawing sessions`);
      
      // Now process each drawing session
      drawingSessions.forEach((session, sessionIndex) => {
        if (session.length < 2) return; // Skip sessions with just one event
        
        const startEvent = session[0];
        const sessionId = `${startEvent.username}-${sessionIndex}`;
        
        // Add to our paths map
        paths.set(sessionId, {
          events: session,
          color: startEvent.color,
          lineWidth: startEvent.lineWidth,
          isEraser: startEvent.isEraser
        });
        
        // Draw each segment within this session
        for (let i = 1; i < session.length; i++) {
          const prevEvent = session[i - 1];
          const currEvent = session[i];
          
          if (currEvent.type === 'DRAW' || currEvent.type === 'ERASE') {
            drawLine(
              prevEvent.x, 
              prevEvent.y, 
              currEvent.x, 
              currEvent.y, 
              currEvent.color, 
              currEvent.lineWidth, 
              currEvent.isEraser
            );
          }
        }
      });
      
      console.log(`Finished processing history batch, rendered ${paths.size} paths`);
    }
    
    function handleRemoteDrawEvent(event: DrawEvent) {
      // Skip our own events as we've already drawn them
      if (event.username === username) return;
      
      if (event.type === 'CLEAR') {
        clearCanvas();
        paths.clear();
        return;
      }
      
      // Generate a timestamp-based session ID for this user's current drawing session
      const sessionPrefix = `${event.username}-session-`;
      
      if (event.type === 'START') {
        // Create a new session ID for this drawing
        const sessionId = `${sessionPrefix}${Date.now()}`;
        
        // Start a new path for this user
        paths.set(sessionId, {
          events: [event],
          color: event.color,
          lineWidth: event.lineWidth,
          isEraser: event.isEraser
        });
      } else if (event.type === 'DRAW' || event.type === 'ERASE') {
        // Find the active session for this user
        let activeSessionId = '';
        let activeSession = null;
        
        // Look for the most recent active session (without END event)
        for (const [pathId, path] of Array.from(paths.entries())) {
          if (pathId.startsWith(sessionPrefix) && 
              !path.events.some(e => e.type === 'END')) {
            
            // Found an active session
            activeSessionId = pathId;
            activeSession = path;
            break;
          }
        }
        
        if (activeSession) {
          // Add to the active session
          const prevEvent = activeSession.events[activeSession.events.length - 1];
          activeSession.events.push(event);
          
          // Draw the line
          drawLine(
            prevEvent.x, 
            prevEvent.y, 
            event.x, 
            event.y, 
            event.color, 
            event.lineWidth, 
            event.isEraser
          );
        } else {
          // No active session found, create a new one with a synthetic START
          const sessionId = `${sessionPrefix}${Date.now()}`;
          const syntheticStart: DrawEvent = {
            ...event,
            type: 'START'
          };
          
          // Create a new path
          paths.set(sessionId, {
            events: [syntheticStart, event],
            color: event.color,
            lineWidth: event.lineWidth,
            isEraser: event.isEraser
          });
          
          // Just draw a point
          drawLine(
            event.x, 
            event.y, 
            event.x, 
            event.y, 
            event.color, 
            event.lineWidth, 
            event.isEraser
          );
        }
      } else if (event.type === 'END') {
        // Find the active session and mark it as ended
        for (const [pathId, path] of Array.from(paths.entries())) {
          if (pathId.startsWith(sessionPrefix) && 
              !path.events.some(e => e.type === 'END')) {
            
            // Mark this session as ended
            path.events.push(event);
            break;
          }
        }
      }
    }
    
    function startDrawing(e: MouseEvent | TouchEvent) {
      isDrawing = true;
      
      const { x, y } = getCoordinates(e);
      
      // Start a new path for the current user
      const startEvent: DrawEvent = {
        username,
        type: 'START',
        x,
        y,
        color,
        lineWidth,
        isEraser
      };
      
      // Send the event to the server
      sendDrawEvent(startEvent);
      
      // Create a session ID for this drawing
      const sessionId = `${username}-session-${Date.now()}`;
      
      // Add to our local paths
      paths.set(sessionId, {
        events: [startEvent],
        color,
        lineWidth,
        isEraser
      });
      
      // Set the initial point
      const abs = toAbsoluteCoordinates(x, y);
      ctx.beginPath();
      ctx.moveTo(abs.x, abs.y);
    }
    
    function draw(e: MouseEvent | TouchEvent) {
      if (!isDrawing) return;
      
      const { x, y } = getCoordinates(e);
      
      // Create draw event
      const drawEvent: DrawEvent = {
        username,
        type: isEraser ? 'ERASE' : 'DRAW',
        x,
        y,
        color,
        lineWidth,
        isEraser
      };
      
      // Send the event to the server
      sendDrawEvent(drawEvent);
      
      // Find the active session for the current user
      const sessionPrefix = `${username}-session-`;
      let activeSession = null;
      
      // Look for the most recent active session
      for (const [pathId, path] of Array.from(paths.entries())) {
        if (pathId.startsWith(sessionPrefix) && 
            !path.events.some(e => e.type === 'END')) {
          activeSession = path;
          break;
        }
      }
      
      if (activeSession) {
        const prevEvent = activeSession.events[activeSession.events.length - 1];
        activeSession.events.push(drawEvent);
        
        // Draw the line
        drawLine(
          prevEvent.x, 
          prevEvent.y, 
          x, 
          y, 
          color, 
          lineWidth, 
          isEraser
        );
      }
    }
    
    function stopDrawing() {
      if (!isDrawing) return;
      
      isDrawing = false;
      
      // Find the active session for the current user
      const sessionPrefix = `${username}-session-`;
      let activeSession = null;
      let lastEvent: DrawEvent | undefined;
      
      // Look for the most recent active session
      for (const [pathId, path] of Array.from(paths.entries())) {
        if (pathId.startsWith(sessionPrefix) && 
            !path.events.some(e => e.type === 'END')) {
          activeSession = path;
          if (path.events.length > 0) {
            lastEvent = path.events[path.events.length - 1];
          }
          break;
        }
      }
      
      if (activeSession && lastEvent) {
        // Create end event
        const endEvent: DrawEvent = {
          username,
          type: 'END',
          x: lastEvent.x,
          y: lastEvent.y,
          color,
          lineWidth,
          isEraser
        };
        
        // Send the event to the server
        sendDrawEvent(endEvent);
        
        // Add to our local path
        activeSession.events.push(endEvent);
      }
    }
    
    function getCoordinates(e: MouseEvent | TouchEvent) {
      let clientX: number, clientY: number;
      
      if ('touches' in e) {
        // Touch event
        e.preventDefault();
        clientX = e.touches[0].clientX;
        clientY = e.touches[0].clientY;
      } else {
        // Mouse event
        clientX = e.clientX;
        clientY = e.clientY;
      }
      
      const rect = canvas.getBoundingClientRect();
      const x = (clientX - rect.left);
      const y = (clientY - rect.top);
      
      // Convert to relative coordinates (percentage of canvas size)
      return toRelativeCoordinates(x, y);
    }
    
    function drawLine(x1: number, y1: number, x2: number, y2: number, color: string, width: number, eraser: boolean) {
      // Convert relative coordinates to absolute for drawing
      const start = toAbsoluteCoordinates(x1, y1);
      const end = toAbsoluteCoordinates(x2, y2);
      
      ctx.globalCompositeOperation = eraser ? 'destination-out' : 'source-over';
      ctx.strokeStyle = color;
      ctx.lineWidth = width;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      
      ctx.beginPath();
      ctx.moveTo(start.x, start.y);
      ctx.lineTo(end.x, end.y);
      ctx.stroke();
    }
    
    function clearCanvas() {
      if (ctx) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        paths.clear();
      }
    }
    
    function redrawCanvas() {
      if (!ctx) return;
      
      clearCanvas();
      
      // Redraw all paths
      paths.forEach(path => {
        const events = path.events;
        
        for (let i = 1; i < events.length; i++) {
          const prev = events[i - 1];
          const curr = events[i];
          
          if (curr.type === 'DRAW' || curr.type === 'ERASE') {
            drawLine(
              prev.x, 
              prev.y, 
              curr.x, 
              curr.y, 
              curr.color, 
              curr.lineWidth, 
              curr.isEraser
            );
          }
        }
      });
    }
    
    function setTool(tool: 'pen' | 'eraser') {
      isEraser = tool === 'eraser';
    }
    
    function handleClearCanvas() {
      // Send clear event
      const clearEvent: DrawEvent = {
        username,
        type: 'CLEAR',
        x: 0,
        y: 0,
        color: '',
        lineWidth: 0,
        isEraser: false
      };
      
      sendDrawEvent(clearEvent);
      clearCanvas();
    }
  </script>
  
  <div class="flex flex-col w-full h-full bg-white rounded-lg overflow-hidden">
    <div class="flex items-center p-2 bg-gray-100 border-b border-gray-300">
      <div class="flex gap-1 mr-4">
        <!-- svelte-ignore a11y_consider_explicit_label -->
        <!-- Lets svelte not freak out about the title attribute -->
        <button 
          class={`flex items-center justify-center w-10 h-10 border-none rounded ${!isEraser ? 'bg-[#840404] text-white' : 'bg-white hover:bg-gray-200'}`}
          on:click={() => setTool('pen')}
          title="Pen">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 19l7-7 3 3-7 7-3-3z"></path>
            <path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"></path>
            <path d="M2 2l7.586 7.586"></path>
            <circle cx="11" cy="11" r="2"></circle>
          </svg>
        </button>
        <!-- svelte-ignore a11y_consider_explicit_label -->
        <button 
          class={`flex items-center justify-center w-10 h-10 border-none rounded ${isEraser ? 'bg-[#840404] text-white' : 'bg-white hover:bg-gray-200'}`}
          on:click={() => setTool('eraser')}
          title="Eraser">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
            <path d="M15.5 2H18a2 2 0 0 1 2 2v2.5"></path>
            <path d="M22 14L13 5"></path>
            <path d="M22 20L13 11"></path>
          </svg>
        </button>
      </div>
      
      <div class="mr-4">
        <input type="color" class="w-10 h-10 border-none rounded cursor-pointer" bind:value={color} disabled={isEraser}>
      </div>
      
      <div class="flex-1 mr-4">
        <input 
          type="range" 
          class="w-full"
          min="1" 
          max="20" 
          bind:value={lineWidth} 
          title="Line Width">
      </div>
      
      <!-- svelte-ignore a11y_consider_explicit_label -->
      <!-- Lets svelte not freak out about the title attribute -->
      <button 
        class="flex items-center justify-center w-10 h-10 border-none bg-white rounded hover:bg-red-100"
        on:click={handleClearCanvas}
        title="Clear Canvas">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 6h18"></path>
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
        </svg>
      </button>
    </div>
    
    <div class="flex-1 relative overflow-hidden">
      <canvas 
        class="absolute inset-0 w-full h-full touch-none"
        bind:this={canvas}
        on:mousedown={startDrawing}
        on:mousemove={draw}
        on:mouseup={stopDrawing}
        on:mouseleave={stopDrawing}
        on:touchstart={startDrawing}
        on:touchmove={draw}
        on:touchend={stopDrawing}
        on:touchcancel={stopDrawing}
      ></canvas>
    </div>
  </div>