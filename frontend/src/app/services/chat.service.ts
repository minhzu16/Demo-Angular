import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

declare var SockJS: any;

export interface ChatMessage {
    type: 'CHAT' | 'JOIN' | 'LEAVE';
    content: string;
    sender: string;
    senderId: number;
    shopId: number;
    shopName?: string;
    timestamp?: string;
}

/**
 * Chat service using raw WebSocket + STOMP protocol frames.
 *
 * Fixes applied:
 * ✅ BUG 23: Tracks active shopId. If connect() is called for a DIFFERENT shop while
 *            already connected, it properly disconnects first to prevent subscription leaks.
 * ✅ BUG 24: Exponential backoff reconnect — automatically tries to reconnect up to
 *            MAX_RETRIES times when the connection is dropped unexpectedly.
 * ✅ BUG 25: Sends senderId as a STOMP header so the backend can verify identity
 *            independently of the message body.
 */
@Injectable({ providedIn: 'root' })
export class ChatService implements OnDestroy {
    private socket: WebSocket | null = null;
    private connected = false;
    private subscriptions: Map<string, (msg: ChatMessage) => void> = new Map();
    private messageCounter = 0;

    // ✅ BUG 23: Track which shopId is currently subscribed
    private currentShopId: number | null = null;
    private currentSender = '';
    private currentSenderId = 0;

    // ✅ BUG 24: Reconnect state
    private reconnectAttempts = 0;
    private readonly MAX_RETRIES = 5;
    private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
    private intentionalDisconnect = false;

    private _messages$ = new BehaviorSubject<ChatMessage[]>([]);
    messages$ = this._messages$.asObservable();

    private _connected$ = new BehaviorSubject<boolean>(false);
    connected$ = this._connected$.asObservable();

    private chatWsUrl = this.getChatWsUrl();

    private getChatWsUrl(): string {
        const host = window.location.hostname;
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        if (host === 'localhost' || host === '127.0.0.1') {
            return 'ws://localhost:8091/ws/chat/websocket';
        }
        return `${protocol}//${host}:8080/ws/chat/websocket`;
    }

    connect(shopId: number, sender: string, senderId: number): void {
        // ✅ BUG 23 FIX: If already connected to a DIFFERENT shop, disconnect first
        if (this.connected && this.currentShopId !== null && this.currentShopId !== shopId) {
            console.log(`[Chat] Switching from shop ${this.currentShopId} to ${shopId} — disconnecting first.`);
            this.disconnectInternal();
        }

        if (this.connected) return;

        this.currentShopId = shopId;
        this.currentSender = sender;
        this.currentSenderId = senderId;
        this.intentionalDisconnect = false;

        this.openSocket(shopId, sender, senderId);
    }

    private openSocket(shopId: number, sender: string, senderId: number): void {
        try {
            this.socket = new WebSocket(this.chatWsUrl);

            this.socket.onopen = () => {
                // ✅ BUG 25 FIX: Send senderId as a trusted STOMP header for server-side verification
                this.sendFrame('CONNECT', {
                    'accept-version': '1.2',
                    'heart-beat': '10000,10000',
                    'senderId': String(senderId),
                    'sender': sender
                });
            };

            this.socket.onmessage = (event: MessageEvent) => {
                const data = event.data as string;
                if (data.startsWith('CONNECTED')) {
                    this.connected = true;
                    this.reconnectAttempts = 0;
                    this._connected$.next(true);
                    this.subscriptions.clear();
                    this.messageCounter = 0;

                    // Subscribe to the shop topic
                    this.stompSubscribe(`/topic/shop/${shopId}`, (msg) => {
                        const current = this._messages$.value;
                        this._messages$.next([...current, msg]);
                    });

                    // Send JOIN notification
                    this.sendMessage(shopId, { type: 'JOIN', content: '', sender, senderId, shopId }, senderId);

                } else if (data.startsWith('MESSAGE')) {
                    this.handleStompMessage(data);
                } else if (data.startsWith('ERROR')) {
                    console.error('[Chat] STOMP ERROR frame received:', data);
                }
            };

            this.socket.onclose = (event: CloseEvent) => {
                this.connected = false;
                this._connected$.next(false);

                // ✅ BUG 24 FIX: Reconnect with exponential backoff if not intentional
                if (!this.intentionalDisconnect && this.reconnectAttempts < this.MAX_RETRIES) {
                    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
                    this.reconnectAttempts++;
                    console.log(`[Chat] Connection closed (code=${event.code}). Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.MAX_RETRIES})...`);
                    this.reconnectTimer = setTimeout(() => {
                        this.openSocket(shopId, sender, senderId);
                    }, delay);
                } else if (this.reconnectAttempts >= this.MAX_RETRIES) {
                    console.warn('[Chat] Max reconnect attempts reached. Giving up.');
                }
            };

            this.socket.onerror = (err) => {
                console.error('[Chat] WebSocket error:', err);
                this.connected = false;
                this._connected$.next(false);
            };

        } catch (e) {
            console.error('[Chat] Failed to create WebSocket:', e);
        }
    }

    sendMessage(shopId: number, message: ChatMessage, senderId?: number): void {
        if (!this.connected || !this.socket) return;
        const body = JSON.stringify(message);
        // ✅ BUG 25 FIX: Include senderId in STOMP SEND header for server-side validation
        const headers: Record<string, string> = {
            destination: `/app/chat/${shopId}`,
            'content-type': 'application/json'
        };
        if (senderId !== undefined) {
            headers['senderId'] = String(senderId);
        }
        this.sendFrame('SEND', headers, body);
    }

    sendChatMessage(shopId: number, content: string, sender: string, senderId: number): void {
        this.sendMessage(shopId, { type: 'CHAT', content, sender, senderId, shopId }, senderId);
    }

    disconnect(): void {
        this.intentionalDisconnect = true;
        this.disconnectInternal();
    }

    /** Internal disconnect without setting intentionalDisconnect flag */
    private disconnectInternal(): void {
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }
        if (this.socket && this.connected) {
            this.sendFrame('DISCONNECT', {});
            this.socket.close();
        }
        this.connected = false;
        this._connected$.next(false);
        this._messages$.next([]);
        this.subscriptions.clear();
        // ✅ BUG 23 FIX: Reset tracked shopId so next connect() works cleanly
        this.currentShopId = null;
    }

    clearMessages(): void {
        this._messages$.next([]);
    }

    ngOnDestroy(): void {
        this.disconnect();
    }

    // --- STOMP frame helpers ---

    private stompSubscribe(destination: string, callback: (msg: ChatMessage) => void): void {
        const id = 'sub-' + (this.messageCounter++);
        this.subscriptions.set(id, callback);
        this.sendFrame('SUBSCRIBE', { id, destination });
    }

    private handleStompMessage(raw: string): void {
        const bodyStart = raw.indexOf('\n\n');
        if (bodyStart === -1) return;
        const body = raw.substring(bodyStart + 2).replace(/\0$/, '');
        try {
            const msg: ChatMessage = JSON.parse(body);
            this.subscriptions.forEach((cb) => cb(msg));
        } catch (e) {
            console.error('[Chat] Failed to parse STOMP message body:', e);
        }
    }

    private sendFrame(command: string, headers: Record<string, string>, body: string = ''): void {
        if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;
        let frame = command + '\n';
        for (const [key, value] of Object.entries(headers)) {
            frame += `${key}:${value}\n`;
        }
        frame += '\n' + body + '\0';
        this.socket.send(frame);
    }
}
