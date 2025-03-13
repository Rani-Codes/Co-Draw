import { Client } from '@stomp/stompjs';
import type { IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// Define the DrawEvent interface to match the backend
export interface DrawEvent {
  username: string;
  type: 'START' | 'DRAW' | 'ERASE' | 'END' | 'CLEAR';
  x: number;
  y: number;
  color: string;
  lineWidth: number;
  isEraser: boolean;
}

// WebSocket client
let stompClient: Client | null = null;

// Connect to WebSocket
export function connect(onMessageReceived: (message: DrawEvent) => void): Promise<void> {
  return new Promise((resolve, reject) => {
    const client = new Client({
      // Use SockJS for compatibility with Spring's STOMP implementation
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      debug: (str) => {
        console.log(str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('Connected to WebSocket');
        
        // Subscribe to the whiteboard topic
        client.subscribe('/topic/whiteboard', (message: IMessage) => {
          if (message.body) {
            const drawEvent: DrawEvent = JSON.parse(message.body);
            onMessageReceived(drawEvent);
          }
        });
        
        stompClient = client;
        resolve();
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame);
        reject(new Error('STOMP error: ' + frame.headers.message));
      }
    });

    client.activate();
  });
}

// Disconnect from WebSocket
export function disconnect(): void {
  if (stompClient) {
    stompClient.deactivate();
    stompClient = null;
    console.log('Disconnected from WebSocket');
  }
}

// Send a draw event to the server
export function sendDrawEvent(drawEvent: DrawEvent): void {
  if (stompClient && stompClient.connected) {
    stompClient.publish({
      destination: '/app/whiteboard.draw',
      body: JSON.stringify(drawEvent)
    });
  } else {
    console.error('Not connected to WebSocket');
  }
}

// Send a clear event to the server
export function sendClearEvent(username: string): void {
  if (stompClient && stompClient.connected) {
    const clearEvent: DrawEvent = {
      username,
      type: 'CLEAR',
      x: 0,
      y: 0,
      color: '',
      lineWidth: 0,
      isEraser: false
    };
    
    stompClient.publish({
      destination: '/app/whiteboard.clear',
      body: JSON.stringify(clearEvent)
    });
  } else {
    console.error('Not connected to WebSocket');
  }
} 