import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService, UserProfile } from '../../services/auth.service';
import { AnalyticsService } from '../../services/analytics.service';
import { ProductService } from '../../services/product.service';
import { WebSocketService } from '../../services/web-socket.service';

import { Subscription } from 'rxjs';

interface FlashParts { h: string; m: string; s: string; }

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

  profile: UserProfile | null = null;
  loading = false;
  flashSaleEndsAt: Date | null = null;
  flashCountdown = '';
  flashParts: FlashParts = { h: '00', m: '00', s: '00' };
  private flashTimerId: any;
  private wsSubscription?: Subscription;

  flashSale: any = null;
  recommendations: any[] = [];
  trending: any[] = [];

  ngOnInit() {
    this.profile = this.auth.getUser();
    this.loadActiveFlashSale();
    this.loadRecommendations();
    this.listenToFlashSaleUpdates();
  }

  listenToFlashSaleUpdates() {
    this.wsSubscription = this.wsService.flashSaleUpdates$.subscribe({
      next: (event) => {
        if (this.flashSale && this.flashSale.id === event.flashSaleId) {
          const item = this.flashSale.items.find((i: any) => i.id === event.productId);
          if (item) {
            item.soldQuantity = event.soldQuantity;
            item.totalQuantity = event.totalQuantity;
          }
        }
      }
    });
  }

  ngOnDestroy(): void {
    if (this.flashTimerId) clearInterval(this.flashTimerId);
    if (this.wsSubscription) this.wsSubscription.unsubscribe();
  }

  logout() { this.auth.logout(); }

  loadActiveFlashSale() {
    this.productService.getActiveFlashSale().subscribe({
      next: (data) => {
        if (data && data.endTime) {
          this.flashSale = data;
          this.flashSaleEndsAt = new Date(data.endTime);
          this.updateFlashCountdown();
          this.flashTimerId = setInterval(() => this.updateFlashCountdown(), 1000);
        }
      },
      error: () => {
        // Fallback mock countdown
        this.initFlashSaleCountdown();
      }
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

  toggleWishlist(product: any) {
    product.wished = !product.wished;
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  }

  getStarArray(n: number): any[] {
    return Array(Math.max(0, Math.floor(n))).fill(0);
  }

  private initFlashSaleCountdown(): void {
    const now = new Date();
    const twoHoursMs = 2 * 60 * 60 * 1000;
    this.flashSaleEndsAt = new Date(now.getTime() + twoHoursMs);
    this.updateFlashCountdown();
    this.flashTimerId = setInterval(() => this.updateFlashCountdown(), 1000);
  }

  private updateFlashCountdown(): void {
    if (!this.flashSaleEndsAt) { this.flashCountdown = ''; return; }

    const diff = this.flashSaleEndsAt.getTime() - Date.now();

    if (diff <= 0) {
      this.flashCountdown = 'Đã kết thúc';
      this.flashParts = { h: '00', m: '00', s: '00' };
      if (this.flashTimerId) clearInterval(this.flashTimerId);
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
