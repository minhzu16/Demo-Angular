import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService, Product } from '../../services/product.service';
import { CartService } from '../../services/cart.service';
import { ReviewService, Review } from '../../services/review.service';
import { AuthService } from '../../services/auth.service';
import { AnalyticsService } from '../../services/analytics.service';
import { ToastrService } from 'ngx-toastr';

import { ChatWidgetComponent } from '../../components/chat-widget/chat-widget.component';

@Component({
    selector: 'app-product-detail',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule, ChatWidgetComponent],
    templateUrl: './product-detail.component.html',
    styleUrls: ['./product-detail.component.scss']
})
export class ProductDetailComponent implements OnInit {
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private productService = inject(ProductService);
    private cartService = inject(CartService);
    private reviewService = inject(ReviewService);
    private authService = inject(AuthService);
    private analyticsService = inject(AnalyticsService);
    private toastr = inject(ToastrService);

    product: any = null;
    variants: any[] = [];
    selectedVariant: any = null;
    quantity = 1;
    loading = false;
    error = '';
    addingToCart = false;

    reviews: Review[] = [];
    totalReviews = 0;
    averageRating = 0;
    reviewsLoading = false;

    newComment = '';
    newRating = 5;
    newMediaUrl = '';
    isLoggedIn = false;

    selectedImage = '';
    images: string[] = [];

    ngOnInit() {
        this.isLoggedIn = this.authService.isAuthenticated();
        
        // ✅ BUG 35 FIX: Subscribe to route params to handle navigation to related products
        this.route.paramMap.subscribe(params => {
            const productId = params.get('id');
            if (productId) {
                const id = +productId;
                this.loadProduct(id);
                this.loadVariants(id);
                this.loadReviews(id);
            }
        });
    }

    loadProduct(id: number) {
        this.loading = true;
        this.error = '';

        this.productService.getProductDetail(id).subscribe({
            next: (product) => {
                this.product = product;
                this.images = product.imageUrl ? [product.imageUrl] : ['assets/placeholder-product.jpg'];
                this.selectedImage = this.images[0];
                this.loading = false;
                this.loadRecommendations();
            },
            error: (err) => {
                this.error = 'Không thể tải thông tin sản phẩm';
                this.loading = false;
            }
        });
    }

    recommendations: any[] = [];

    loadRecommendations() {
        // Lấy sản phẩm liên quan (tạm thời dùng trending list nếu không có list riêng)
        // Hoặc tìm kiếm cùng category
        if (this.isLoggedIn) {
             const user = this.authService.getUser();
             if (user) {
                 this.analyticsService.getRecommendations(user.id).subscribe({
                     next: (res) => this.recommendations = res?.slice(0, 4) || [],
                     error: () => this.loadRelatedProductsFallback()
                 });
             } else {
                 this.loadRelatedProductsFallback();
             }
        } else {
             this.loadRelatedProductsFallback();
        }
    }

    loadRelatedProductsFallback() {
        if (this.product?.categoryId) {
             this.productService.searchProducts({ category: this.product.categoryId, page: 0, size: 4 }).subscribe({
                  next: (res: any) => {
                      this.recommendations = (res.content || res).filter((p: any) => p.id !== this.product.id).slice(0, 4);
                  }
             });
        } else {
             this.analyticsService.getTrending().subscribe({
                  next: (res) => {
                      this.recommendations = res.filter(p => p.id !== this.product.id).slice(0, 4);
                  }
             });
        }
    }

    loadVariants(productId: number) {
        this.productService.getProductVariants(productId).subscribe({
            next: (variants) => {
                this.variants = variants || [];
                if (this.variants.length > 0) {
                    this.selectedVariant = this.variants[0];
                }
            },
            error: () => {
                this.variants = [];
            }
        });
    }

    selectImage(image: string) {
        this.selectedImage = image;
    }

    selectVariant(variant: any) {
        this.selectedVariant = variant;
    }

    increaseQuantity() {
        if (this.quantity < 99) {
            this.quantity++;
        }
    }

    decreaseQuantity() {
        if (this.quantity > 1) {
            this.quantity--;
        }
    }

    addToCart() {
        if (!this.product) return;

        this.addingToCart = true;

        this.cartService.addItem(this.product.id, this.quantity).subscribe({
            next: () => {
                this.addingToCart = false;
                this.toastr.success('Đã thêm vào giỏ hàng!');
            },
            error: (err) => {
                this.addingToCart = false;
                this.toastr.error('Không thể thêm vào giỏ hàng. Vui lòng thử lại.');
            }
        });
    }

    buyNow() {
        this.addToCart();
        setTimeout(() => {
            this.router.navigate(['/cart']);
        }, 500);
    }

    goBack() {
        this.router.navigate(['/products']);
    }

    formatPrice(price: number): string {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
    }

    loadReviews(id: number) {
        this.reviewsLoading = true;
        this.reviewService.getProductReviews(id, 0, 5).subscribe({
            next: (res) => {
                this.reviews = res.content || [];
                this.totalReviews = res.totalElements;
                this.reviewsLoading = false;
                // Average Rating now comes from product object itself, kept in sync by backend
                if (this.product) {
                    this.averageRating = this.product.averageRating || 0;
                }
            },
            error: () => {
                this.reviewsLoading = false;
            }
        });
    }

    submitReview() {
        if (!this.newComment.trim()) return;

        const profile = this.authService.getUser();
        const review: Review = {
            productId: this.product.id,
            userId: profile?.id || 0,
            userName: profile?.username || 'Guest',
            rating: this.newRating,
            comment: this.newComment,
            mediaUrls: this.newMediaUrl ? this.newMediaUrl.split(',').map(u => u.trim()).filter(u => u) : []
        };

        this.reviewService.createReview(review).subscribe({
            next: (saved) => {
                this.reviews.unshift(saved);
                this.totalReviews++;
                this.newComment = '';
                this.newMediaUrl = '';
                this.newRating = 5;
                this.toastr.success('Cảm ơn bạn đã đánh giá!');
            },
            error: () => this.toastr.error('Không thể gửi đánh giá. Vui lòng thử lại.')
        });
    }

    getStarArray(n: number): any[] {
        return Array(Math.max(0, Math.floor(n))).fill(0);
    }
}
