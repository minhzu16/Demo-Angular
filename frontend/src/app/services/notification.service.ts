import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import { tap } from 'rxjs/operators';

export interface Notification {
  id: number;
  message: string;
  title?: string;
  type: string;
  isRead: boolean;
  targetUrl?: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private apiUrl = `${environment.apiBaseUrl}/notifications`;

  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  notifications$ = this.notificationsSubject.asObservable();

  private unreadCountSubject = new BehaviorSubject<number>(0);
  unreadCount$ = this.unreadCountSubject.asObservable();

  private getHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  loadNotifications(): void {
    const user = this.authService.getUser();
    if (!user) return;
    this.http.get<any>(`${this.apiUrl}/user/${user.id}`, { headers: this.getHeaders() })
      .subscribe(res => {
        // Handle pagination object if returned
        const notifications = res.content || res;
        this.notificationsSubject.next(notifications);
        this.updateUnreadCount(notifications);
      });
  }

  markAsRead(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/read`, {}, { headers: this.getHeaders() })
      .pipe(
        tap(() => {
          const current = this.notificationsSubject.value;
          const updated = current.map(n => n.id === id ? { ...n, isRead: true } : n);
          this.notificationsSubject.next(updated);
          this.updateUnreadCount(updated);
        })
      );
  }

  markAllAsRead(): Observable<void> {
    const user = this.authService.getUser();
    if (!user) return new Observable<void>();
    return this.http.put<void>(`${this.apiUrl}/user/${user.id}/read-all`, {}, { headers: this.getHeaders() })
      .pipe(
        tap(() => {
          const current = this.notificationsSubject.value;
          const updated = current.map(n => ({ ...n, isRead: true }));
          this.notificationsSubject.next(updated);
          this.unreadCountSubject.next(0);
        })
      );
  }

  private updateUnreadCount(notifications: Notification[]): void {
    const count = notifications.filter(n => !n.isRead).length;
    this.unreadCountSubject.next(count);
  }

  // Placeholder for WebSocket STOMP integration
  // Developers should install stompjs and sockjs-client to enable real-time notifications
  connectWebSocket(): void {
    console.log('Real-time notifications via WebSocket (STOMP) ready to be implemented.');
    // Implementation would go here...
  }
}
