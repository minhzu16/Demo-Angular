import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { OrderService, Order } from '../../services/order.service';
import { AuthService } from '../../services/auth.service';

@Component({
    selector: 'app-orders',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './orders.component.html',
    styleUrls: ['./orders.component.scss']
})
export class OrdersComponent implements OnInit {
    private orderService = inject(OrderService);
    private authService = inject(AuthService);

    orders: Order[] = [];
    loading = false;
    error = '';

    ngOnInit() {
        this.loadOrders();
    }

    loadOrders() {
        this.loading = true;
        const user = this.authService.getUser();
        if (!user) {
            this.error = 'Vui lòng đăng nhập để xem đơn hàng';
            return;
        }

        this.orderService.getMyOrders(user.id).subscribe({
            next: (response: any) => {
                this.orders = response.content || response || [];
                // Sort by newest first
                this.orders.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
                this.loading = false;
            },
            error: (err) => {
                this.error = 'Không thể tải danh sách đơn hàng';
                this.loading = false;
            }
        });
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

    formatPrice(price: number): string {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
    }

    formatDate(date: string): string {
        return new Date(date).toLocaleDateString('vi-VN', {
            year: 'numeric', month: '2-digit', day: '2-digit',
            hour: '2-digit', minute: '2-digit'
        });
    }
}
