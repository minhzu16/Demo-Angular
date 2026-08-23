import { Component, Input, OnInit, OnDestroy, OnChanges, SimpleChanges, inject, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService, ChatMessage } from '../../services/chat.service';
import { AuthService } from '../../services/auth.service';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-chat-widget',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './chat-widget.component.html',
    styleUrls: ['./chat-widget.component.scss']
})
export class ChatWidgetComponent implements OnInit, OnChanges, OnDestroy, AfterViewChecked {
    @Input() shopId!: number;
    @Input() shopName: string = 'Shop';

    @ViewChild('messagesContainer') messagesContainer!: ElementRef;

    private chatService = inject(ChatService);
    private authService = inject(AuthService);

    isOpen = false;
    isConnected = false;
    messages: ChatMessage[] = [];
    newMessage = '';
    currentUser = '';
    currentUserId = 0;

    private messagesSub!: Subscription;
    private connectedSub!: Subscription;
    private shouldScroll = false;

    ngOnInit(): void {
        const user = this.authService.getUser();
        this.currentUser = user?.username || user?.fullName || 'Guest';
        this.currentUserId = user?.id || 0;

        this.messagesSub = this.chatService.messages$.subscribe(msgs => {
            this.messages = msgs;
            this.shouldScroll = true;
        });

        this.connectedSub = this.chatService.connected$.subscribe(connected => {
            this.isConnected = connected;
        });
    }

    ngAfterViewChecked(): void {
        if (this.shouldScroll) {
            this.scrollToBottom();
            this.shouldScroll = false;
        }
    }

    /**
     * ✅ BUG 23 FIX: When shopId input changes (user navigates to a different shop's page),
     * we must disconnect from the old shop channel before reconnecting to the new one.
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (changes['shopId'] && !changes['shopId'].isFirstChange()) {
            const prev = changes['shopId'].previousValue;
            const curr = changes['shopId'].currentValue;
            if (prev !== curr) {
                this.isOpen = false;
                this.chatService.disconnect();
            }
        }
    }

    toggleChat(): void {
        this.isOpen = !this.isOpen;
        if (this.isOpen && !this.isConnected && this.shopId) {
            this.chatService.connect(this.shopId, this.currentUser, this.currentUserId);
        }
    }

    sendMessage(): void {
        if (!this.newMessage.trim() || !this.shopId) return;
        this.chatService.sendChatMessage(this.shopId, this.newMessage.trim(), this.currentUser, this.currentUserId);
        this.newMessage = '';
    }

    onKeyDown(event: KeyboardEvent): void {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.sendMessage();
        }
    }

    closeChat(): void {
        this.isOpen = false;
    }

    isOwnMessage(msg: ChatMessage): boolean {
        return msg.senderId === this.currentUserId;
    }

    formatTime(timestamp: string | undefined): string {
        if (!timestamp) return '';
        try {
            const date = new Date(timestamp);
            return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
        } catch {
            return '';
        }
    }

    private scrollToBottom(): void {
        try {
            if (this.messagesContainer?.nativeElement) {
                this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
            }
        } catch (err) {
            // Ignore scroll errors
        }
    }

    ngOnDestroy(): void {
        this.messagesSub?.unsubscribe();
        this.connectedSub?.unsubscribe();
        this.chatService.disconnect();
    }
}
