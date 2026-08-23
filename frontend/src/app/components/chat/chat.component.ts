import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WebSocketService } from '../../services/web-socket.service';
import { AuthService } from '../../services/auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss'
})
export class ChatComponent implements OnInit, OnDestroy {
  isOpen = false;
  messages: any[] = [];
  newMessage = '';
  receiverId: number = 1; // Default to admin or a specific seller
  currentUser: any;
  private chatSub?: Subscription;

  constructor(
    private webSocketService: WebSocketService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.currentUser = this.authService.getUser();
    if (this.currentUser) {
      this.chatSub = this.webSocketService.chatMessages$.subscribe(msg => {
        this.messages.push(msg);
        setTimeout(() => this.scrollToBottom(), 100);
      });
    }
  }

  ngOnDestroy() {
    if (this.chatSub) {
      this.chatSub.unsubscribe();
    }
  }

  toggleChat() {
    this.isOpen = !this.isOpen;
  }

  sendMessage() {
    if (!this.newMessage.trim() || !this.currentUser) return;
    
    const msg = {
      senderId: this.currentUser.id,
      receiverId: this.receiverId,
      content: this.newMessage,
      senderName: this.currentUser.username || 'User'
    };
    
    this.webSocketService.sendChatMessage(msg);
    this.newMessage = '';
  }

  scrollToBottom() {
    const chatBody = document.querySelector('.chat-body');
    if (chatBody) {
      chatBody.scrollTop = chatBody.scrollHeight;
    }
  }
}
