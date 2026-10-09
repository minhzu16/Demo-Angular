import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { catchError, forkJoin, of, switchMap } from 'rxjs';
import { ProductService, Product } from '../../services/product.service';
import { CartService } from '../../services/cart.service';
import { WishlistService } from '../../services/wishlist.service';

@Component({
  selector: 'app-wishlist',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './wishlist.component.html',
  styleUrls: ['./wishlist.component.scss']
})
export class WishlistComponent implements OnInit {
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private wishlistService = inject(WishlistService);
  private toastr = inject(ToastrService);

  products: Product[] = [];
  loading = true;
  failed = false;
  addingToCart: number | null = null;

  ngOnInit() {
    this.loadWishlist();
  }

  loadWishlist() {
    this.loading = true;
    this.failed = false;

    // The wishlist API stores product ids only, so resolve each id to its product card data.
    this.wishlistService.list().pipe(
      switchMap(items => items.length
        ? forkJoin(items.map(i => this.productService.getProductDetail(i.productId).pipe(catchError(() => of(null)))))
        : of([] as (Product | null)[]))
    ).subscribe({
      next: products => {
        this.products = products.filter((p): p is Product => !!p);
        this.loading = false;
      },
      error: () => {
        this.failed = true;
        this.loading = false;
      }
    });
  }

  remove(product: Product) {
    this.wishlistService.remove(product.id).subscribe({
      next: () => {
        this.products = this.products.filter(p => p.id !== product.id);
        this.toastr.info('Đã bỏ khỏi danh sách yêu thích');
      },
      error: () => this.toastr.error('Không thể cập nhật danh sách yêu thích. Vui lòng thử lại.')
    });
  }

  addToCart(product: Product) {
    this.addingToCart = product.id;
    this.cartService.addItem(product.id, 1).subscribe({
      next: () => {
        this.addingToCart = null;
        this.toastr.success('Đã thêm vào giỏ hàng');
      },
      error: () => {
        this.addingToCart = null;
        this.toastr.error('Không thể thêm vào giỏ hàng. Vui lòng thử lại.');
      }
    });
  }

  imageOf(product: Product): string {
    return product.imageUrl || product.thumbnailUrl || 'assets/placeholder-product.jpg';
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  }
}
