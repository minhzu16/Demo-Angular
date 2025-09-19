import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-nav-right',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './nav-right.component.html',
  styleUrl: './nav-right.component.scss'
})
export class NavRightComponent {
  notifications = [
    { id: 1, message: 'New order received', time: '2 min ago', unread: true },
    { id: 2, message: 'Payment processed', time: '5 min ago', unread: true },
    { id: 3, message: 'Customer inquiry', time: '1 hour ago', unread: false }
  ];

  unreadCount = this.notifications.filter(n => n.unread).length;
}
