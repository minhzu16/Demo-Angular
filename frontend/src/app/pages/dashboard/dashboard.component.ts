import { Component, inject, OnInit, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { CardComponent } from '../../components/card/card.component';
import { BarChartComponent } from '../../components/charts/bar-chart/bar-chart.component';
import { PieChartComponent } from '../../components/charts/pie-chart/pie-chart.component';
import { AuthService } from '../../services/auth.service';
import { ShopService } from '../../services/shop.service';
import { OrderService, OrderListResponse } from '../../services/order.service';
import { WarehouseService } from '../../services/warehouse.service';
import { AnalyticsService } from '../../services/analytics.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, CardComponent, BarChartComponent, PieChartComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, AfterViewInit {
  private router = inject(Router);
  private authService = inject(AuthService);
  private shopService = inject(ShopService);
  private orderService = inject(OrderService);
  private warehouseService = inject(WarehouseService);
  private analyticsService = inject(AnalyticsService);
  private toastr = inject(ToastrService);

  currentShopId: number | null = null;
  
  // Chart Data
  revenueData: number[] = [];
  revenueLabels: string[] = [];
  statusData: number[] = [];
  statusLabels: string[] = [];

  ngOnInit(): void {
    this.loadDashboardData();
  }

  ngAfterViewInit(): void {
    // Ensure dashboard view is rendered before showing alert
    setTimeout(() => {
      const state: any = history.state || {};
      const fromLogin = state.fromLogin === true;
      const nameFromState = state.fullName as string | undefined;

      const token = this.authService.getToken();
      const welcomeKey = `welcome_shown_${token ? token.slice(0, 10) : 'guest'}`;
      const hasShown = sessionStorage.getItem(welcomeKey);

      const show = (name?: string) => {
        if (hasShown) return;
        if (name) this.toastr.success(`Chào mừng, ${name}!`);
        sessionStorage.setItem(welcomeKey, 'true');
      };

      if (fromLogin) {
        if (nameFromState) {
          show(nameFromState);
          return;
        }
        const user = this.authService.getUser();
        if (user) {
          show(user.fullName || user.username);
          return;
        }
      }

      const existingUser = this.authService.getUser();
      if (existingUser) {
        show(existingUser.fullName || existingUser.username);
        return;
      }
      this.authService.getProfile().subscribe({
        next: profile => {
          this.authService.saveUser(profile);
          show(profile.fullName || profile.username);
        },
        error: () => sessionStorage.setItem(welcomeKey, 'true')
      });
    }, 0);
  }


  goToProfile() {
    this.router.navigate(['/profile']).catch(err => {
      console.error('Profile navigation error:', err);
    });
  }

  private loadDashboardData(): void {
    this.shopService.getMyShop().subscribe({
      next: (shop: any) => {
        this.currentShopId = shop.id;
        this.loadOrderStats(shop.id);
        this.loadStockStats(shop.id);
        this.loadRecentOrders(shop.id);
        this.loadChartData(shop.id);
      },
      error: (err: any) => {
        console.error('Error loading seller shop for dashboard:', err);
      }
    });
  }

  private loadOrderStats(shopId: number): void {
    this.orderService.getOrderStatistics(shopId).subscribe({
      next: (stats: any) => {
        const revenue = (stats.todayRevenue ?? stats.totalRevenue ?? 0).toLocaleString('vi-VN');
        const totalOrders = (stats.totalOrders ?? 0).toString();

        this.stats = [
          { ...this.stats[0], title: 'Total Revenue', value: `${revenue} ₫`, badge: '', description: '' },
          { ...this.stats[1], title: 'Total Orders', value: totalOrders, badge: '', description: '' },
          this.stats[2],
          this.stats[3]
        ];
      },
      error: (err: any) => {
        console.error('Error loading order statistics for dashboard:', err);
      }
    });
  }

  private loadStockStats(shopId: number): void {
    this.warehouseService.getShopProductStats(shopId).subscribe({
      next: (stats: any) => {
        const lowStock = (stats.lowStockProducts ?? 0).toString();
        const outOfStock = (stats.outOfStockProducts ?? 0).toString();
        this.stats = [
          this.stats[0],
          this.stats[1],
          { ...this.stats[2], title: 'Low Stock Products', value: lowStock },
          { ...this.stats[3], title: 'Out of Stock Products', value: outOfStock }
        ];
      },
      error: (err: any) => {
        console.error('Error loading stock statistics for dashboard:', err);
      }
    });
  }

  private loadRecentOrders(shopId: number): void {
    this.orderService.getShopOrders(shopId).subscribe({
      next: (res: OrderListResponse) => {
        const orders = res.content || [];
        this.recentOrders = orders.slice(0, 5).map(o => ({
          id: `#${o.id}`,
          customer: `User ${o.userId}`,
          product: o.items?.[0]?.productName || 'N/A',
          amount: (o.totalAmount || 0).toLocaleString('vi-VN') + ' ₫',
          status: o.status
        }));
      },
      error: (err: any) => {
        console.error('Error loading recent orders for dashboard:', err);
      }
    });
  }

  private loadChartData(shopId: number): void {
    // 1. Get Revenue Data for Bar Chart
    const end = new Date().toISOString().split('T')[0];
    const start = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    
    this.analyticsService.getRevenue(shopId, start, end, 'daily').subscribe({
      next: (data: any[]) => {
        this.revenueLabels = data.map(d => new Date(d.date).toLocaleDateString('vi-VN', { weekday: 'short' }));
        this.revenueData = data.map(d => d.revenue);
      }
    });

    // 2. Get Dashboard / Order Distribution for Pie Chart
    this.analyticsService.getDashboard(shopId).subscribe({
      next: (dashboard: any) => {
        const stats = dashboard.orderStats || {};
        this.statusLabels = ['Completed', 'Pending', 'Cancelled'];
        this.statusData = [
          stats.completedOrders || 0, 
          stats.pendingOrders || 0, 
          stats.cancelledOrders || 0
        ];
      }
    });
  }

  stats = [
    {
      title: 'Total Revenue',
      value: '--',
      description: 'Đang tải...',
      icon: 'dollar-circle',
      badge: '',
      type: 'stat' as const,
      badgeType: 'success' as const
    },
    {
      title: 'Total Orders',
      value: '--',
      description: 'Đang tải...',
      icon: 'shopping-cart',
      badge: '',
      type: 'stat' as const,
      badgeType: 'info' as const
    },
    {
      title: 'Low Stock Products',
      value: '--',
      description: 'Đang tải...',
      icon: 'user',
      badge: '',
      type: 'stat' as const,
      badgeType: 'warning' as const
    },
    {
      title: 'Out of Stock Products',
      value: '--',
      description: 'Đang tải...',
      icon: 'line-chart',
      badge: '',
      type: 'stat' as const,
      badgeType: 'danger' as const
    }
  ];

  recentOrders: { id: string; customer: string; product: string; amount: string; status: string }[] = [];

  transactions = [
    { id: 1, title: 'Payment from John Doe', time: '2 min ago', amount: '+$1,299', percentage: '+12.5%', avatar: 'JD' },
    { id: 2, title: 'Payment from Jane Smith', time: '5 min ago', amount: '+$699', percentage: '+8.2%', avatar: 'JS' },
    { id: 3, title: 'Payment from Bob Johnson', time: '1 hour ago', amount: '+$499', percentage: '+15.3%', avatar: 'BJ' },
    { id: 4, title: 'Payment from Alice Brown', time: '2 hours ago', amount: '+$199', percentage: '+5.1%', avatar: 'AB' },
    { id: 5, title: 'Payment from Charlie Wilson', time: '3 hours ago', amount: '+$399', percentage: '+9.8%', avatar: 'CW' }
  ];

  supportTeam = [
    { name: 'Sarah', avatar: 'S' },
    { name: 'Mike', avatar: 'M' },
    { name: 'Lisa', avatar: 'L' },
    { name: 'Tom', avatar: 'T' }
  ];

  getStatusClass(status: string): string {
    switch (status) {
      case 'completed': return 'success';
      case 'pending': return 'warning';
      case 'shipped': return 'info';
      default: return 'secondary';
    }
  }

  getAvatarColor(index: number): string {
    const colors = ['bg-primary', 'bg-success', 'bg-warning', 'bg-danger', 'bg-info'];
    return colors[index % colors.length];
  }
}
