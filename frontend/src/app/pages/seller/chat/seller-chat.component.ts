import { Component, OnInit, OnDestroy, inject, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ChatService, ChatMessage } from '../../../services/chat.service';
import { AuthService } from '../../../services/auth.service';
import { ShopService } from '../../../services/shop.service';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-seller-chat',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterModule],
    templateUrl: './seller-chat.component.html',
    styleUrls: ['./seller-chat.component.scss']
})
export class SellerChatComponent implements OnInit, OnDestroy, AfterViewChecked {
    @ViewChild('messagesContainer') messagesContainer!: ElementRef;

    private chatService = inject(ChatService);
    private authService = inject(AuthService);
    private shopService = inject(ShopService);

    shop: any = null;
    isConnected = false;
    messages: ChatMessage[] = [];
    newMessage = '';
    currentUser = '';
    currentUserId = 0;
    loading = true;
    error = '';

    private messagesSub!: Subscription;
    private connectedSub!: Subscription;
    private shouldScroll = false;

    ngOnInit(): void {
        const user = this.authService.getUser();
        this.currentUser = user?.username || user?.fullName || 'Seller';
        this.currentUserId = user?.id || 0;

        this.messagesSub = this.chatService.messages$.subscribe(msgs => {
            this.messages = msgs;
            this.shouldScroll = true;
        });

        this.connectedSub = this.chatService.connected$.subscribe(connected => {
            this.isConnected = connected;
        });

        this.loadShopAndConnect();
    }

    ngAfterViewChecked(): void {
        if (this.shouldScroll) {
            this.scrollToBottom();
            this.shouldScroll = false;
        }
    }

    loadShopAndConnect(): void {
        this.shopService.getMyShop().subscribe({
            next: (shop) => {
                this.shop = shop;
                this.loading = false;
                // Auto-connect to own shop's chat channel
                this.chatService.connect(shop.id, this.currentUser, this.currentUserId);
            },
            error: () => {
                this.error = 'Không thể tải thông tin cửa hàng. Bạn cần có cửa hàng để sử dụng tính năng chat.';
                this.loading = false;
            }
        });
    }

    sendMessage(): void {
        if (!this.newMessage.trim() || !this.shop) return;
        this.chatService.sendChatMessage(this.shop.id, this.newMessage.trim(), this.currentUser, this.currentUserId);
        this.newMessage = '';
    }

    onKeyDown(event: KeyboardEvent): void {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.sendMessage();
        }
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
            // Ignore
        }
    }

    ngOnDestroy(): void {
        this.messagesSub?.unsubscribe();
        this.connectedSub?.unsubscribe();
        this.chatService.disconnect();
    }
}
