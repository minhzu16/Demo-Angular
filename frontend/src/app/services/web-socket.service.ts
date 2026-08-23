import { Injectable, inject } from '@angular/core';
import { Subject, Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private authService = inject(AuthService);
  private socket: WebSocket | null = null;
  private notificationSubject = new Subject<any>();
  private flashSaleSubject = new Subject<any>();
  private chatSubject = new Subject<any>();

  public notifications$ = this.notificationSubject.asObservable();
  public flashSaleUpdates$ = this.flashSaleSubject.asObservable();
  public chatMessages$ = this.chatSubject.asObservable();

  connect() {
    const user = this.authService.getUser();
    if (!user) return;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    // Use the API Gateway address
    const host = window.location.host === 'localhost:4200' ? 'localhost:8080' : window.location.host;
    const url = `${protocol}//${host}/ws-notifications/websocket`;

    console.log('Connecting to WebSocket:', url);
    this.socket = new WebSocket(url);

    this.socket.onopen = () => {
      console.log('WebSocket connected');
      // Simple STOMP CONNECT frame
      this.socket?.send('CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n\0');
    };

    this.socket.onmessage = (event) => {
      const data = event.data;
      if (data.startsWith('CONNECTED')) {
        console.log('STOMP Connected');
        // Subscribe to user notifications
        this.socket?.send(`SUBSCRIBE\nid:sub-0\ndestination:/topic/notifications/${user.id}\n\n\0`);
        // Subscribe to flash sale updates (public)
        this.socket?.send(`SUBSCRIBE\nid:sub-1\ndestination:/topic/flash-sale\n\n\0`);
        // Subscribe to chat messages
        this.socket?.send(`SUBSCRIBE\nid:sub-chat\ndestination:/topic/chat/${user.id}\n\n\0`);
      } else if (data.includes('MESSAGE')) {
        // Extract JSON body from STOMP frame
        const bodyMatch = data.match(/\n\n([\s\S]*?)\0/);
        if (bodyMatch && bodyMatch[1]) {
          try {
            const body = JSON.parse(bodyMatch[1]);
            
            if (data.includes('destination:/topic/flash-sale')) {
              this.flashSaleSubject.next(body);
            } else if (data.includes(`destination:/topic/chat/${user.id}`)) {
              this.chatSubject.next(body);
            } else {
              this.notificationSubject.next(body);
            }
          } catch (e) {
            console.error('Error parsing WebSocket message:', e);
          }
        }
      }
    };

    this.socket.onclose = () => {
      console.log('WebSocket connection closed');
      // Attempt reconnect after 5 seconds
      setTimeout(() => this.connect(), 5000);
    };

    this.socket.onerror = (error) => {
      console.error('WebSocket error:', error);
    };
  }

  disconnect() {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
  }

  sendChatMessage(message: any) {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      const payload = JSON.stringify(message);
      const stompMessage = `SEND\ndestination:/app/chat.sendMessage\ncontent-type:application/json\ncontent-length:${payload.length}\n\n${payload}\0`;
      this.socket.send(stompMessage);
    } else {
      console.error('WebSocket is not connected');
    }
  }
}
