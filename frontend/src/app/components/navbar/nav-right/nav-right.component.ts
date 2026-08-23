import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService, UserProfile } from '../../../services/auth.service';
import { Router, RouterModule } from '@angular/router';
import { NotificationService, Notification } from '../../../services/notification.service';
import { formatDistanceToNow } from 'date-fns';
import { vi } from 'date-fns/locale';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-nav-right',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './nav-right.component.html',
  styleUrls: ['./nav-right.component.scss']
})
export class NavRightComponent implements OnInit {
  public authService = inject(AuthService);
  public notificationService = inject(NotificationService);
  private router = inject(Router);
  public translate = inject(TranslateService);

  user: UserProfile | null = null;
  notifications: Notification[] = [];
  unreadCount = 0;

  ngOnInit() {
    this.user = this.authService.getUser();
    this.authService.getProfile().subscribe(profile => {
      this.user = profile;
    });

    this.notificationService.notifications$.subscribe(data => {
      this.notifications = data;
    });
    this.notificationService.unreadCount$.subscribe(count => {
      this.unreadCount = count;
    });

    this.notificationService.loadNotifications();
  }

  logout() {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }

  markAllAsRead() {
    this.notificationService.markAllAsRead().subscribe();
  }

  getTimeAgo(date: string): string {
    if (!date) return '';
    try {
      return formatDistanceToNow(new Date(date), { addSuffix: true, locale: vi });
    } catch (e) {
      return 'Vừa xong';
    }
  }

  changeLang(lang: string) {
    this.translate.use(lang);
  }
}
