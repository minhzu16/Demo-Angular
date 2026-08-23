import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartService, Cart, CartItem } from '../../services/cart.service';
import { AuthService } from '../../services/auth.service';
import { VoucherService, Voucher, VoucherValidationResponse } from '../../services/voucher.service';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'app-cart',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule],
    templateUrl: './cart.component.html',
    styleUrls: ['./cart.component.scss']
})
export class CartComponent implements OnInit {
    private cartService = inject(CartService);
    private authService = inject(AuthService);
    private router = inject(Router);
    private toastr = inject(ToastrService);
    private voucherService = inject(VoucherService);

    cart: Cart = { items: [], totalItems: 0, totalAmount: 0 };
    loading = false;
    error = '';
    updatingItem: number | null = null;

    // Voucher state
    voucherCode = '';
    appliedVoucher: Voucher | null = null;
    discountAmount = 0;
    validatingVoucher = false;

    ngOnInit() {
        this.loadCart();
    }

    loadCart() {
        this.loading = true;
        this.error = '';

        const user = this.authService.getUser();
        const userId = user?.id;

        this.cartService.getCart(userId).subscribe({
            next: (cart) => {
                this.cart = cart || { items: [], totalItems: 0, totalAmount: 0 };
                
                // Initialize selection state
                if (this.cart.items) {
                    this.cart.items.forEach(item => {
                        if (item.selected === undefined) item.selected = true; // Default select all
                    });
                }
                this.recalculateSelectedTotals();

                this.loading = false;
                // Re-validate voucher silently if cart total changed
                if (this.appliedVoucher && this.cart) {
                    this.silentRevalidateVoucher();
                }
            },
            error: (err) => {
                this.error = 'Không thể tải giỏ hàng';
                this.loading = false;
            }
        });
    }

    // --- New Selection Logic ---
    selectAll = true;
    selectedItemsCount = 0;
    selectedTotalAmount = 0;

    toggleSelectAll() {
        this.selectAll = !this.selectAll;
        this.cart.items.forEach(item => item.selected = this.selectAll);
        this.recalculateSelectedTotals();
    }

    toggleSelectItem(item: CartItem) {
        item.selected = !item.selected;
        this.selectAll = this.cart.items.every(i => i.selected);
        this.recalculateSelectedTotals();
    }

    recalculateSelectedTotals() {
        this.selectedItemsCount = this.cart.items.filter(i => i.selected).reduce((sum, item) => sum + item.quantity, 0);
        this.selectedTotalAmount = this.cart.items.filter(i => i.selected).reduce((sum, item) => sum + (item.price * item.quantity), 0);
        
        // Remove voucher if the selected total goes to 0 or changes significantly
        if (this.selectedTotalAmount === 0 && this.appliedVoucher) {
             this.removeVoucher();
        } else if (this.appliedVoucher) {
             this.silentRevalidateVoucher();
        }
    }

    updateQuantity(item: CartItem, newQuantity: number) {
        if (newQuantity < 1 || newQuantity > 99) return;

        this.updatingItem = item.productId;
        this.cartService.updateQuantity(item.productId, newQuantity).subscribe({
            next: () => {
                this.loadCart();
                this.updatingItem = null;
            },
            error: (err) => {
                this.toastr.error('Không thể cập nhật số lượng');
                this.updatingItem = null;
            }
        });
    }

    removeItem(productId: number) {
        if (!confirm('Bạn có chắc muốn xóa sản phẩm này?')) return;

        this.cartService.removeItem(productId).subscribe({
            next: () => {
                this.loadCart();
            },
            error: (err) => {
                this.toastr.error('Không thể xóa sản phẩm');
            }
        });
    }

    clearCart() {
        if (!confirm('Bạn có chắc muốn xóa toàn bộ giỏ hàng?')) return;

        this.cartService.clearCart().subscribe({
            next: () => {
                this.loadCart();
            },
            error: (err) => {
                this.toastr.error('Không thể xóa giỏ hàng');
            }
        });
    }

    proceedToCheckout() {
        const selectedItems = this.cart?.items?.filter(i => i.selected) || [];
        if (selectedItems.length === 0) {
            this.toastr.warning('Vui lòng chọn ít nhất một sản phẩm để thanh toán');
            return;
        }

        // Pass voucher state and selected items via navigation matrix or state object
        const navigationExtras = {
            state: {
                voucherCode: this.appliedVoucher ? this.voucherCode : undefined,
                discountAmount: this.discountAmount,
                selectedProductIds: selectedItems.map(i => i.productId)
            }
        };

        this.router.navigate(['/checkout'], navigationExtras);
    }

    continueShopping() {
        this.router.navigate(['/products']);
    }

    formatPrice(price: number): string {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
    }

    getItemTotal(item: CartItem): number {
        return item.price * item.quantity;
    }

    applyVoucher() {
        if (!this.voucherCode.trim()) {
            this.toastr.warning('Vui lòng nhập mã giảm giá');
            return;
        }

        if (this.selectedTotalAmount === 0) {
            this.toastr.warning('Vui lòng chọn sản phẩm trước khi áp dụng mã giảm giá');
            return;
        }

        this.validatingVoucher = true;
        this.voucherService.validateVoucher(this.voucherCode, this.selectedTotalAmount).subscribe({
            next: (res: VoucherValidationResponse) => {
                this.validatingVoucher = false;
                if (res.valid) {
                    this.appliedVoucher = res.voucher || null;
                    this.discountAmount = res.discountAmount || 0;
                    this.toastr.success(`Áp dụng thành công! Giảm ${this.formatPrice(this.discountAmount)}`);
                } else {
                    this.toastr.error(res.message || 'Mã giảm giá không hợp lệ');
                    this.removeVoucher();
                }
            },
            error: () => {
                this.validatingVoucher = false;
                this.toastr.error('Không thể kiểm tra mã giảm giá lúc này');
                this.removeVoucher();
            }
        });
    }

    removeVoucher() {
        this.voucherCode = '';
        this.appliedVoucher = null;
        this.discountAmount = 0;
    }

    private silentRevalidateVoucher() {
        if (this.selectedTotalAmount === 0 || !this.voucherCode) return;
        this.voucherService.validateVoucher(this.voucherCode, this.selectedTotalAmount).subscribe({
            next: (res) => {
                if (res.valid) {
                    this.discountAmount = res.discountAmount || 0;
                } else {
                    this.toastr.warning('Mã giảm giá không còn hiệu lực cho đơn hàng này');
                    this.removeVoucher();
                }
            },
            error: () => this.removeVoucher()
        });
    }
}
