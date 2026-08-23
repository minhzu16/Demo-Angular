import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ShopService } from '../../../services/shop.service';
import { OrderService } from '../../../services/order.service';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'app-seller-orders',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule],
    templateUrl: './seller-orders.component.html',
    styleUrls: ['./seller-orders.component.scss']
})
export class SellerOrdersComponent implements OnInit {
    private shopService = inject(ShopService);
    private orderService = inject(OrderService);
    private toastr = inject(ToastrService);

    orders: any[] = [];
    loading = false;
    error = '';
    shopId: number | null = null;
    statusFilter = 'ALL';

    ngOnInit() {
        this.loadShopAndOrders();
    }

    loadShopAndOrders() {
        this.loading = true;
        this.shopService.getMyShop().subscribe({
            next: (shop) => {
                this.shopId = shop.id;
                this.loadOrders(shop.id);
            },
            error: () => {
                this.error = 'Không thể tải thông tin cửa hàng';
                this.loading = false;
            }
        });
    }

    loadOrders(shopId: number) {
        this.loading = true;
        this.orderService.getShopOrders(shopId, 0, 20).subscribe({
            next: (res: any) => {
                this.orders = res.content || res || [];
                // Filter locally for now
                if (this.statusFilter !== 'ALL') {
                    this.orders = this.orders.filter(o => o.status === this.statusFilter);
                }
                this.loading = false;
            },
            error: () => {
                this.error = 'Không thể tải danh sách đơn hàng';
                this.loading = false;
            }
        });
    }

    filterOrders(status: string) {
        this.statusFilter = status;
        if (this.shopId) {
            this.loadOrders(this.shopId);
        }
    }

    updateOrderStatus(orderId: number, newStatus: string) {
        if (!confirm(`Bạn có chắc muốn chuyển trạng thái đơn hàng sang ${this.getStatusText(newStatus)}?`)) return;

        this.orderService.updateOrderStatus(orderId, newStatus).subscribe({
            next: () => {
                this.toastr.success(`Đã cập nhật trạng thái đơn hàng #${orderId} sang ${this.getStatusText(newStatus)}`);
                if (this.shopId) {
                    this.loadOrders(this.shopId);
                }
            },
            error: (err) => {
                this.toastr.error(err.error?.message || 'Không thể cập nhật trạng thái đơn hàng');
            }
        });
    }

    formatPrice(price: number): string {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
    }

    formatDate(dateStr: string): string {
        return new Date(dateStr).toLocaleDateString('vi-VN', {
            year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
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
}
