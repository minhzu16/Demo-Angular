import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CartService, Cart } from '../../services/cart.service';
import { AuthService } from '../../services/auth.service';
import { OrderService, OrderItem } from '../../services/order.service';
import { PaymentService } from '../../services/payment.service';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'app-checkout',
    standalone: true,
    imports: [CommonModule, RouterModule, ReactiveFormsModule],
    templateUrl: './checkout.component.html',
    styleUrls: ['./checkout.component.scss']
})
export class CheckoutComponent implements OnInit {
    private fb = inject(FormBuilder);
    private cartService = inject(CartService);
    private authService = inject(AuthService);
    private orderService = inject(OrderService);
    private router = inject(Router);
    private paymentService = inject(PaymentService);
    private toastr = inject(ToastrService);

    cart: Cart = { items: [], totalItems: 0, totalAmount: 0 };
    addresses: any[] = [];
    loading = false;
    processing = false;
    error = '';

    // Loyalty Points
    userPoints = 0;
    usePoints = 0;
    pointsDiscount = 0;
    pointsRedeemed = false;

    // Inherited from Cart state
    voucherCode?: string;
    discountAmount = 0;
    selectedProductIds: number[] = [];

    checkoutForm = this.fb.group({
        fullName: ['', Validators.required],
        phoneNumber: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
        address: ['', Validators.required],
        note: [''],
        paymentMethod: ['COD', Validators.required]
    });

    paymentMethods = [
        { id: 'COD', name: 'Thanh toán khi nhận hàng (COD)', icon: 'bi-cash' },
        { id: 'VNPAY', name: 'Thanh toán qua VNPay', icon: 'bi-credit-card' },
        { id: 'SEPAY', name: 'Chuyển khoản VietQR (SePay)', icon: 'bi-qr-code-scan' }
    ];

    ngOnInit() {
        // Retrieve Voucher State From Router Payload
        const navigation = this.router.getCurrentNavigation();
        if (navigation?.extras.state) {
            this.voucherCode = navigation.extras.state['voucherCode'];
            this.discountAmount = navigation.extras.state['discountAmount'] || 0;
            this.selectedProductIds = navigation.extras.state['selectedProductIds'] || [];
        } else {
            // Backup fallback for hard refreshes
            this.voucherCode = history.state.voucherCode;
            this.discountAmount = history.state.discountAmount || 0;
            this.selectedProductIds = history.state.selectedProductIds || [];
        }

        this.loadCart();
        this.loadAddresses();
        this.loadUserPoints();
    }

    loadUserPoints() {
        this.authService.getProfile().subscribe({
            next: (profile) => {
                this.userPoints = profile.loyaltyPoints || 0;
            }
        });
    }

    togglePoints() {
        if (!this.pointsRedeemed) {
            // Maximum points that can be used is the order total (1 point = 1 VND)
            const maxUsable = this.cart.totalAmount - this.discountAmount;
            this.usePoints = Math.min(this.userPoints, maxUsable);
            this.pointsDiscount = this.usePoints;
            this.pointsRedeemed = true;
        } else {
            this.usePoints = 0;
            this.pointsDiscount = 0;
            this.pointsRedeemed = false;
        }
    }

    loadCart() {
        this.loading = true;
        const user = this.authService.getUser();
        this.cartService.getCart(user?.id).subscribe({
            next: (cart) => {
                this.cart = cart || { items: [], totalItems: 0, totalAmount: 0 };
                
                // Filter only selected items
                if (this.selectedProductIds.length > 0 && this.cart.items) {
                    this.cart.items = this.cart.items.filter(item => this.selectedProductIds.includes(item.productId));
                    // Recalculate totals for selected items
                    this.cart.totalAmount = this.cart.items.reduce((sum, item) => sum + (item.price * item.quantity), 0);
                    this.cart.totalItems = this.cart.items.reduce((sum, item) => sum + item.quantity, 0);
                }

                this.loading = false;
                if (!cart || !cart.items || cart.items.length === 0 || this.cart.items.length === 0) {
                    this.router.navigate(['/cart']);
                }
            },
            error: () => {
                this.error = 'Không thể tải thông tin giỏ hàng';
                this.loading = false;
            }
        });
    }

    loadAddresses() {
        this.authService.getAddresses().subscribe({
            next: (data) => {
                this.addresses = data;
                this.prefillForm();
            },
            error: () => console.log('Không thể tải địa chỉ')
        });
    }

    prefillForm() {
        const user = this.authService.getUser();
        
        // Priority 1: Default saved address
        const defaultAddr = this.addresses.find(a => a.isDefault);
        if (defaultAddr) {
            this.selectAddress(defaultAddr);
            return;
        }

        // Priority 2: First saved address
        if (this.addresses.length > 0) {
            this.selectAddress(this.addresses[0]);
            return;
        }

        // Priority 3: Basic user profile info
        if (user) {
            this.checkoutForm.patchValue({
                fullName: user.fullName || user.username,
                phoneNumber: user.phoneNumber,
                address: user.address
            });
        }
    }

    selectAddress(addr: any) {
        this.checkoutForm.patchValue({
            fullName: addr.receiverName,
            phoneNumber: addr.phoneNumber,
            address: addr.address
        });
    }

    submitOrder() {
        if (this.checkoutForm.invalid || !this.cart) return;

        this.processing = true;
        const user = this.authService.getUser();

        // Map cart items to order items
        if (!this.cart.items || this.cart.items.length === 0) {
            this.error = 'Giỏ hàng của bạn đang trống hoặc không hợp lệ.';
            this.processing = false;
            return;
        }

        const orderItems: OrderItem[] = (this.cart?.items || []).map(item => ({
            productId: item.productId,
            quantity: item.quantity,
            price: item.price,
            productName: item.productName,
            imageUrl: item.imageUrl
        }));

        const request = {
            userId: user?.id || 0,
            shippingAddress: {
                fullName: this.checkoutForm.value.fullName!,
                phoneNumber: this.checkoutForm.value.phoneNumber!,
                street: this.checkoutForm.value.address!
            },
            paymentMethod: this.checkoutForm.value.paymentMethod!,
            items: orderItems,
            note: this.checkoutForm.value.note || '',
            voucherCode: this.voucherCode || undefined,
            usePoints: this.usePoints > 0 ? this.usePoints : undefined
        };

        this.orderService.createOrder(request).subscribe({
            next: (order) => {
                const method = this.checkoutForm.value.paymentMethod;
                if (method === 'COD') {
                    this.completeOrder(order.id);
                } else {
                    this.processPayment(order.id, order.totalAmount, method!);
                }
            },
            error: (err) => {
                const errorMsg = err?.error?.message || err?.error?.error || 'Đặt hàng thất bại. Vui lòng thử lại.';
                this.error = errorMsg;
                this.processing = false;
            }
        });
    }

    processPayment(orderId: number, amount: number, method: string) {
        this.paymentService.createPayment({
            orderId: orderId,
            amount: amount,
            currency: 'VND',
            paymentMethod: method
        }).subscribe({
            next: (res: any) => {
                const url = res.redirectUrl || res.paymentUrl || res.url;
                
                if (method === 'VNPAY' && url) {
                    this.cartService.clearCart().subscribe();
                    window.location.href = url;
                } else if (method === 'SEPAY') {
                    this.cartService.clearCart().subscribe();
                    this.router.navigate(['/orders', orderId, 'success'], {
                        state: { paymentMethod: 'SEPAY', sepayQrUrl: url || '' }
                    });
                } else {
                    this.completeOrder(orderId);
                }
            },
            error: () => {
                this.error = 'Khởi tạo thanh toán thất bại. Đơn hàng của bạn vẫn được ghi nhận.';
                this.completeOrder(orderId);
            }
        });
    }

    completeOrder(orderId?: number) {
        this.cartService.clearCart().subscribe();
        this.processing = false;
        const targetId = orderId || 0;
        if (targetId > 0) {
            this.router.navigate(['/orders', targetId, 'success'], {
                state: { paymentMethod: this.checkoutForm.value.paymentMethod }
            });
        } else {
            this.router.navigate(['/orders']);
        }
    }

    formatPrice(price: number): string {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
    }
}
