import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { OrderService, Order } from '../../services/order.service';
import { PaymentService, PaymentDto } from '../../services/payment.service';

@Component({
    selector: 'app-order-success',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './order-success.component.html',
    styleUrls: ['./order-success.component.scss']
})
export class OrderSuccessComponent implements OnInit {
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private orderService = inject(OrderService);
    private paymentService = inject(PaymentService);

    order: Order | null = null;
    payment: PaymentDto | null = null;
    loading = true;
    error = '';

    // SePay QR URL
    sepayQrUrl = '';
    paymentMethod = '';
    orderId = 0;

    ngOnInit(): void {
        const idParam = this.route.snapshot.paramMap.get('id');
        if (!idParam) {
            this.router.navigate(['/orders']);
            return;
        }
        this.orderId = +idParam;

        // Check router state for payment info passed from checkout
        const nav = this.router.getCurrentNavigation();
        const state = nav?.extras?.state || history.state;
        this.paymentMethod = state?.['paymentMethod'] || '';
        this.sepayQrUrl = state?.['sepayQrUrl'] || '';

        this.loadOrder();
        this.loadPayment();
    }

    loadOrder(): void {
        this.orderService.getOrder(this.orderId).subscribe({
            next: (order) => {
                this.order = order;
                this.paymentMethod = this.paymentMethod || order.paymentMethod || 'COD';
                this.loading = false;
            },
            error: () => {
                this.error = 'Không thể tải thông tin đơn hàng';
                this.loading = false;
            }
        });
    }

    loadPayment(): void {
        this.paymentService.getPaymentInfo(this.orderId).subscribe({
            next: (payment) => {
                this.payment = payment;
                if (payment.paymentMethod === 'SEPAY' && payment.redirectUrl) {
                    this.sepayQrUrl = payment.redirectUrl;
                    this.paymentMethod = 'SEPAY';
                }
            },
            error: () => {
                // Payment record might not exist for COD; that's ok
            }
        });
    }

    formatPrice(price: number): string {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
    }

    getStatusText(status: string): string {
        const map: Record<string, string> = {
            'PENDING': 'Chờ xác nhận',
            'CONFIRMED': 'Đã xác nhận',
            'SHIPPING': 'Đang giao',
            'COMPLETED': 'Hoàn thành',
            'CANCELLED': 'Đã hủy',
            'PAID': 'Đã thanh toán'
        };
        return map[status] || status;
    }

    getPaymentMethodText(method: string): string {
        const map: Record<string, string> = {
            'COD': 'Thanh toán khi nhận hàng',
            'VNPAY': 'VNPay',
            'SEPAY': 'Chuyển khoản VietQR (SePay)'
        };
        return map[method] || method;
    }
}
