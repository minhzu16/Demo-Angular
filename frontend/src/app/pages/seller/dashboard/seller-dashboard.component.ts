import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ShopService } from '../../../services/shop.service';
import { OrderService } from '../../../services/order.service';
import { AuthService } from '../../../services/auth.service';
import { AnalyticsService } from '../../../services/analytics.service';
import { WarehouseService } from '../../../services/warehouse.service';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';

@Component({
    selector: 'app-seller-dashboard',
    standalone: true,
    imports: [CommonModule, RouterModule, NgChartsModule],
    templateUrl: './seller-dashboard.component.html',
    styleUrls: ['./seller-dashboard.component.scss']
})
export class SellerDashboardComponent implements OnInit {
    private shopService = inject(ShopService);
    private orderService = inject(OrderService);
    private authService = inject(AuthService);
    private analyticsService = inject(AnalyticsService);
    private warehouseService = inject(WarehouseService);

    shop: any = null;
    stats: any = null;
    warehouseStats: any = null;
    recentOrders: any[] = [];
    topProducts: any[] = [];
    loading = false;
    error = '';

    // Chart configuration
    public lineChartData: ChartData<'line'> = {
        labels: [],
        datasets: [
            {
                data: [],
                label: 'Doanh thu (VNĐ)',
                fill: true,
                tension: 0.4,
                borderColor: '#4e73df',
                backgroundColor: 'rgba(78, 115, 223, 0.05)',
                pointBackgroundColor: '#4e73df'
            }
        ]
    };
    public lineChartOptions: ChartConfiguration['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: { display: false }
        },
        scales: {
            y: {
                beginAtZero: true
            }
        }
    };

    ngOnInit() {
        this.loadDashboardData();
    }

    loadDashboardData() {
        this.loading = true;
        this.shopService.getMyShop().subscribe({
            next: (shop) => {
                this.shop = shop;
                this.loadStats(shop.id);
                this.loadWarehouseStats(shop.id);
                this.loadRecentOrders(shop.id);
                this.loadRevenueChart(shop.id);
                this.loadTopProducts(shop.id);
            },
            error: () => {
                this.error = 'Bạn chưa có cửa hàng hoặc không thể tải thông tin';
                this.loading = false;
            }
        });
    }

    loadWarehouseStats(shopId: number) {
        this.warehouseService.getShopProductStats(shopId).subscribe({
            next: (stats) => {
                this.warehouseStats = stats;
            },
            error: () => console.error('Failed to load warehouse stats')
        });
    }

    loadStats(shopId: number) {
        this.orderService.getShopOrderStats(shopId).subscribe({
            next: (stats) => {
                this.stats = stats;
            },
            error: () => {
                console.error('Failed to load stats');
            }
        });
    }

    loadRecentOrders(shopId: number) {
        this.orderService.getShopOrders(shopId, 0, 5).subscribe({
            next: (res: any) => {
                this.recentOrders = res.content || res || [];
                this.loading = false;
            },
            error: () => {
                this.loading = false;
            }
        });
    }

    loadRevenueChart(shopId: number) {
        const today = new Date();
        const past = new Date();
        past.setDate(past.getDate() - 7); // Last 7 days
        
        const start = past.toISOString().split('T')[0];
        const end = today.toISOString().split('T')[0];

        this.analyticsService.getRevenue(shopId, start, end, 'daily').subscribe({
            next: (data) => {
                const labels = data.map(d => d.date || d.period);
                const values = data.map(d => d.revenue);
                
                this.lineChartData = {
                    labels: labels,
                    datasets: [
                        {
                            ...this.lineChartData.datasets[0],
                            data: values
                        }
                    ]
                };
            },
            error: () => console.error('Failed to load revenue chart')
        });
    }

    loadTopProducts(shopId: number) {
        this.analyticsService.getTopProducts(shopId, 5, 30).subscribe({
            next: (data) => {
                this.topProducts = data;
            },
            error: () => console.error('Failed to load top products')
        });
    }

    formatPrice(price: number): string {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
    }

    getStatusBadgeClass(status: string): string {
        switch (status) {
            case 'PENDING': return 'bg-warning text-dark';
            case 'CONFIRMED': return 'bg-info text-white';
            case 'SHIPPING': return 'bg-primary';
            case 'COMPLETED': return 'bg-success';
            case 'CANCELLED': return 'bg-danger';
            default: return 'bg-secondary';
        }
    }

    getStatusText(status: string): string {
        switch (status) {
            case 'PENDING': return 'Chờ xác nhận';
            case 'CONFIRMED': return 'Đã xác nhận';
            case 'SHIPPING': return 'Đang giao hàng';
            case 'COMPLETED': return 'Hoàn thành';
            case 'CANCELLED': return 'Đã hủy';
            default: return status;
        }
    }
}
