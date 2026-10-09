import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { AuthService, UserProfile } from '../../services/auth.service';
import { AnalyticsService } from '../../services/analytics.service';
import { ProductService } from '../../services/product.service';
import { WebSocketService } from '../../services/web-socket.service';
import { WishlistService } from '../../services/wishlist.service';

import { Subscription } from 'rxjs';

interface FlashParts { h: string; m: string; s: string; }

interface HomeCategory {
  name: string;
  icon: string;
  tone: 'vermilion' | 'jade' | 'gold' | 'indigo' | 'ink' | 'paper';
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslateModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
  private auth = inject(AuthService);
  private analytics = inject(AnalyticsService);
  private productService = inject(ProductService);
  private wsService = inject(WebSocketService);
  private wishlist = inject(WishlistService);
  private toastr = inject(ToastrService);
  private router = inject(Router);

  profile: UserProfile | null = null;
  loading = false;
  flashSaleEndsAt: Date | null = null;
  flashCountdown = '';
  flashParts: FlashParts = { h: '00', m: '00', s: '00' };
  private flashTimerId: any;
  private wsSubscription?: Subscription;

  flashSale: any = null;
  flashLoaded = false;
  recommendations: any[] = [];
  trending: any[] = [];

  readonly categories: HomeCategory[] = [
    { name: 'Điện tử',    icon: 'bi-phone',      tone: 'indigo' },
    { name: 'Thời trang', icon: 'bi-bag',        tone: 'vermilion' },
    { name: 'Nhà bếp',    icon: 'bi-house',      tone: 'jade' },
    { name: 'Sách',       icon: 'bi-book',       tone: 'gold' },
    { name: 'Thể thao',   icon: 'bi-bicycle',    tone: 'ink' },
    { name: 'Mỹ phẩm',    icon: 'bi-stars',      tone: 'vermilion' },
    { name: 'Đồ chơi',    icon: 'bi-controller', tone: 'jade' },
  ];

  ngOnInit() {
    this.profile = this.auth.getUser();
    this.loadActiveFlashSale();
    this.loadRecommendations();
    this.listenToFlashSaleUpdates();
    this.wishlist.ensureLoaded();
  }

  listenToFlashSaleUpdates() {
    this.wsSubscription = this.wsService.flashSaleUpdates$.subscribe({
      next: (event) => {
        if (this.flashSale && this.flashSale.id === event.flashSaleId) {
          const item = this.flashSale.items?.find((i: any) => i.id === event.productId);
          if (item) {
            item.soldQuantity = event.soldQuantity;
            item.totalQuantity = event.totalQuantity;
          }
        }
      }
    });
  }

  ngOnDestroy(): void {
    this.stopFlashTimer();
    if (this.wsSubscription) this.wsSubscription.unsubscribe();
  }

  logout() { this.auth.logout(); }

  loadActiveFlashSale() {
    this.productService.getActiveFlashSale().subscribe({
      next: (data) => {
        this.flashLoaded = true;
        if (data && data.endTime) {
          this.flashSale = data;
          this.flashSaleEndsAt = new Date(data.endTime);
          this.updateFlashCountdown();
          this.stopFlashTimer();
          this.flashTimerId = setInterval(() => this.updateFlashCountdown(), 1000);
        }
      },
      // No invented countdown: when there is no active flash sale we say so.
      error: () => { this.flashLoaded = true; }
    });
  }

  loadRecommendations() {
    const user = this.auth.getUser();
    if (user) {
      this.analytics.getRecommendations(user.id).subscribe({
        next: data => this.recommendations = data?.slice(0, 8) || [],
        error: () => {}
      });
    }
    this.analytics.getTrending().subscribe({
      next: data => this.trending = data?.slice(0, 8) || [],
      error: () => {}
    });
  }

  isWished(product: any): boolean {
    return this.wishlist.has(Number(product.id));
  }

  toggleWishlist(product: any) {
    if (!this.auth.isAuthenticated()) {
      this.toastr.info('Đăng nhập để lưu sản phẩm yêu thích');
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    this.wishlist.toggle(Number(product.id)).subscribe({
      next: wished => this.toastr.success(wished ? 'Đã thêm vào yêu thích' : 'Đã bỏ khỏi yêu thích'),
      error: () => this.toastr.error('Không thể cập nhật yêu thích. Vui lòng thử lại.')
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  }

  getStarArray(n: number): any[] {
    return Array(Math.max(0, Math.floor(n))).fill(0);
  }

  /** Always 5 stars in total: 4.8 → 5 filled, 4.2 → 4 filled + 1 empty. */
  filledStars(rating: number): any[] {
    return this.getStarArray(Math.min(5, Math.max(0, Math.round(rating || 0))));
  }

  emptyStars(rating: number): any[] {
    return this.getStarArray(5 - Math.min(5, Math.max(0, Math.round(rating || 0))));
  }

  flashProgress(item: any): number {
    const total = Number(item?.totalQuantity) || 0;
    if (total <= 0) return 0;
    return Math.min(100, Math.max(0, (Number(item.soldQuantity) / total) * 100));
  }

  get topFlashItem(): any {
    return this.flashSale?.items?.[0] ?? null;
  }

  private stopFlashTimer(): void {
    if (this.flashTimerId) {
      clearInterval(this.flashTimerId);
      this.flashTimerId = null;
    }
  }

  private updateFlashCountdown(): void {
    if (!this.flashSaleEndsAt) { this.flashCountdown = ''; return; }

    const diff = this.flashSaleEndsAt.getTime() - Date.now();

    if (diff <= 0) {
      this.flashCountdown = 'Đã kết thúc';
      this.flashParts = { h: '00', m: '00', s: '00' };
      this.stopFlashTimer();
      return;
    }

    const totalSeconds = Math.floor(diff / 1000);
    const h = Math.floor(totalSeconds / 3600);
    const m = Math.floor((totalSeconds % 3600) / 60);
    const s = totalSeconds % 60;

    this.flashParts = {
      h: h.toString().padStart(2, '0'),
      m: m.toString().padStart(2, '0'),
      s: s.toString().padStart(2, '0'),
    };
    this.flashCountdown = `${this.flashParts.h}:${this.flashParts.m}:${this.flashParts.s}`;
  }
}
