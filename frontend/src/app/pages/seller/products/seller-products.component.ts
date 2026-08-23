import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ShopService } from '../../../services/shop.service';
import { ProductService, Product } from '../../../services/product.service';
import { AuthService } from '../../../services/auth.service';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'app-seller-products',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './seller-products.component.html',
    styleUrls: ['./seller-products.component.scss']
})
export class SellerProductsComponent implements OnInit {
    private shopService = inject(ShopService);
    private productService = inject(ProductService);
    private authService = inject(AuthService);
    private toastr = inject(ToastrService);

    products: Product[] = [];
    loading = false;
    error = '';
    shopId: number | null = null;

    ngOnInit() {
        this.loadShopAndProducts();
    }

    loadShopAndProducts() {
        this.loading = true;
        this.shopService.getMyShop().subscribe({
            next: (shop) => {
                this.shopId = shop.id;
                this.loadProducts(shop.id);
            },
            error: () => {
                this.error = 'Không thể tải thông tin cửa hàng';
                this.loading = false;
            }
        });
    }

    loadProducts(shopId: number) {
        const user = this.authService.getUser();
        if (!user) return;
        
        this.productService.getSellerProducts(user.id).subscribe({
            next: (res: any) => {
                this.products = res.content || res || [];
                this.loading = false;
            },
            error: () => {
                this.error = 'Không thể tải danh sách sản phẩm';
                this.loading = false;
            }
        });
    }

    deleteProduct(id: number) {
        if (!confirm('Bạn có chắc muốn xóa sản phẩm này?')) return;

        this.productService.deleteProduct(id).subscribe({
            next: () => {
                this.toastr.success('Đã xóa sản phẩm');
                if (this.shopId) this.loadProducts(this.shopId);
            },
            error: () => this.toastr.error('Lỗi khi xóa sản phẩm')
        });
    }

    formatPrice(price: number): string {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
    }
}
