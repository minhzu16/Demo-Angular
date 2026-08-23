import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ProductService, Product } from '../../services/product.service';
import { CartService } from '../../services/cart.service';

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

  products: Product[] = [];
  loading = true;
  addingToCart: number | null = null;

  ngOnInit() {
    this.loadWishlist();
  }

  loadWishlist() {
    // Load wishlist products via product service (featured as fallback)
    this.productService.getFeaturedProducts(12).subscribe({
      next: res => {
        this.products = res.content || [];
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  addToCart(product: Product) {
    this.addingToCart = product.id;
    this.cartService.addItem(product.id, 1).subscribe({
      next: () => { this.addingToCart = null; },
      error: () => { this.addingToCart = null; }
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  }
}
