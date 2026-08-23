import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { OrderService, Order } from '../../services/order.service';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.scss']
})
export class OrderDetailComponent implements OnInit {
  private orderService = inject(OrderService);
  private route = inject(ActivatedRoute);

  order: Order | null = null;
  loading = true;
  error = '';

  readonly statusSteps = ['PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED'];

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) { this.error = 'Mã đơn hàng không hợp lệ'; this.loading = false; return; }
    this.orderService.getOrder(id).subscribe({
      next: order => { this.order = order; this.loading = false; },
      error: () => { this.error = 'Không tìm thấy đơn hàng'; this.loading = false; }
    });
  }

  getStatusText(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận',
      SHIPPING: 'Đang giao hàng', COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy'
    };
    return map[status] ?? status;
  }

  getStatusIcon(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'bi-clock', CONFIRMED: 'bi-check-circle',
      SHIPPING: 'bi-truck', COMPLETED: 'bi-bag-check', CANCELLED: 'bi-x-circle'
    };
    return map[status] ?? 'bi-circle';
  }

  currentStepIndex(): number {
    if (!this.order) return -1;
    return this.statusSteps.indexOf(this.order.status);
  }

  formatPrice(p: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(p);
  }

  formatDate(d: string): string {
    return new Date(d).toLocaleDateString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
  }
}
