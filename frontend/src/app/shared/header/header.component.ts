import { Component, inject, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService, UserProfile } from '../../services/auth.service';
import { CartService, Cart } from '../../services/cart.service';
import { NotificationService, Notification } from '../../services/notification.service';
import { ProductService } from '../../services/product.service';
import { Subject, takeUntil } from 'rxjs';

interface Category {
  name: string;
  icon?: string;
}

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent implements OnInit, OnDestroy {
  private auth = inject(AuthService);
  private router = inject(Router);
  private cartService = inject(CartService);
  private notificationService = inject(NotificationService);
  private productService = inject(ProductService);

  profile: UserProfile | null = null;
  searchQuery = '';
  searchFocused = false;
  cartItemsCount = 0;
  cartBounce = false;
  notifications: Notification[] = [];
  unreadNotificationsCount = 0;
  showNotificationsDropdown = false;
  showUserMenu = false;
  isScrolled = false;
  recentSearches: string[] = [];

  categories: Category[] = [
    { name: 'Điện tử',    icon: 'bi-phone' },
    { name: 'Thời trang', icon: 'bi-bag' },
    { name: 'Nhà bếp',   icon: 'bi-house' },
    { name: 'Sách',       icon: 'bi-book' },
    { name: 'Thể thao',  icon: 'bi-bicycle' },
    { name: 'Mỹ phẩm',   icon: 'bi-stars' },
    { name: 'Đồ chơi',   icon: 'bi-controller' },
    { name: 'Ô tô',      icon: 'bi-car-front' },
  ];

  private destroy$ = new Subject<void>();

  @HostListener('window:scroll')
  onScroll(): void {
    this.isScrolled = window.scrollY > 20;
  }

  ngOnInit(): void {
    this.loadRecentSearches();
    this.refreshAuthStatus();
    if (this.auth.isAuthenticated()) {
      this.loadCartCount();
      this.loadNotifications();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  refreshAuthStatus(): void {
    this.profile = this.auth.getUser();
    if (this.auth.isAuthenticated() && !this.profile) {
      this.auth.getProfile().subscribe({
        next: (res: any) => {
          this.profile = res;
          this.auth.saveUser(res);
        },
        error: (err: any) => console.warn('Profile fetch skipped', err)
      });
    }
  }

  loadCartCount(): void {
    this.cartService.getCart().subscribe({
      next: (cart: Cart | null) => {
        const newCount = cart && cart.items ? cart.items.length : 0;
        if (newCount > this.cartItemsCount) {
          this.cartBounce = true;
          setTimeout(() => this.cartBounce = false, 500);
        }
        this.cartItemsCount = newCount;
      },
      error: () => this.cartItemsCount = 0
    });
  }

  loadNotifications(): void {
    const userId = this.profile?.id;
    if (!userId) return;
    this.notificationService.loadNotifications();
    this.notificationService.notifications$.pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.notifications = res || [];
    });
    this.notificationService.unreadCount$.pipe(takeUntil(this.destroy$)).subscribe(count => {
      this.unreadNotificationsCount = count;
    });
  }

  onNotificationToggle(): void {
    this.showNotificationsDropdown = !this.showNotificationsDropdown;
    this.showUserMenu = false;
    if (this.showNotificationsDropdown) {
      this.loadNotifications();
    }
  }

  markAsRead(notif: Notification): void {
    if (!notif.id) return;
    this.notificationService.markAsRead(notif.id).subscribe(() => {
      notif.isRead = true;
      this.unreadNotificationsCount = Math.max(0, this.unreadNotificationsCount - 1);
      if (notif.targetUrl) {
        this.router.navigateByUrl(notif.targetUrl);
        this.showNotificationsDropdown = false;
      }
    });
  }

  markAllRead(): void {
    const userId = this.profile?.id;
    if (!userId) return;
    this.notificationService.markAllAsRead().subscribe(() => {
      this.notifications.forEach(n => n.isRead = true);
      this.unreadNotificationsCount = 0;
    });
  }

  getNotifIcon(type?: string): string {
    const map: Record<string, string> = {
      ORDER_STATUS: 'bi-box-seam',
      FLASH_SALE:   'bi-lightning-charge-fill',
      REVIEW:       'bi-chat-square-text',
      VOUCHER:      'bi-ticket-perforated',
      SYSTEM:       'bi-megaphone',
    };
    return map[type?.toUpperCase() || ''] || 'bi-bell';
  }

  onSearch(): void {
    const q = this.searchQuery.trim();
    if (!q) return;
    this.saveRecentSearch(q);
    this.searchFocused = false;
    this.router.navigate(['/products'], { queryParams: { q } });
  }

  selectSearch(s: string): void {
    this.searchQuery = s;
    this.onSearch();
  }

  onSearchBlur(): void {
    setTimeout(() => this.searchFocused = false, 200);
  }

  private loadRecentSearches(): void {
    try {
      const stored = localStorage.getItem('nx_recent_searches');
      this.recentSearches = stored ? JSON.parse(stored) : [];
    } catch { this.recentSearches = []; }
  }

  private saveRecentSearch(query: string): void {
    this.recentSearches = [query, ...this.recentSearches.filter(s => s !== query)].slice(0, 5);
    localStorage.setItem('nx_recent_searches', JSON.stringify(this.recentSearches));
  }

  getAvatarLetter(): string {
    const name = this.profile?.fullName || this.profile?.username || '?';
    return name.charAt(0).toUpperCase();
  }

  logout(): void {
    this.auth.logout();
    this.profile = null;
    this.cartItemsCount = 0;
    this.unreadNotificationsCount = 0;
    this.notifications = [];
    this.showUserMenu = false;
    this.router.navigate(['/home']);
  }
}
