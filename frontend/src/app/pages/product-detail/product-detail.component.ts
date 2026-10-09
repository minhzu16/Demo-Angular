import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService, Product } from '../../services/product.service';
import { CartService } from '../../services/cart.service';
import { ReviewService, Review } from '../../services/review.service';
import { AuthService } from '../../services/auth.service';
import { AnalyticsService } from '../../services/analytics.service';
import { WishlistService } from '../../services/wishlist.service';
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
    private wishlistService = inject(WishlistService);
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
        this.wishlistService.ensureLoaded();

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
                this.product = this.normalizeProduct(product);
                this.selectedImage = this.images[0];
                this.quantity = 1;
                this.loading = false;
                this.averageRating = this.product.averageRating || 0;
                this.loadRecommendations();
            },
            error: (err) => {
                this.error = 'Không thể tải thông tin sản phẩm';
                this.loading = false;
            }
        });
    }

    /**
     * Maps the API's ProductDetailDTO onto what the template renders:
     * gallery from thumbnailUrl + images[].url (the API has no `imageUrl`),
     * strike-through price from listPrice, discount %, and specs from attributesJson.
     */
    private normalizeProduct(p: any): any {
        const gallery = (Array.isArray(p?.images) ? [...p.images] : [])
            .sort((a: any, b: any) => (a?.sortOrder ?? 0) - (b?.sortOrder ?? 0))
            .map((img: any) => (typeof img === 'string' ? img : img?.url))
            .filter((url: any): url is string => !!url);
        const primary = p?.imageUrl || p?.thumbnailUrl;
        this.images = Array.from(new Set([primary, ...gallery].filter((u): u is string => !!u)));
        if (this.images.length === 0) this.images = ['assets/placeholder-product.jpg'];

        const original = p?.originalPrice ?? p?.listPrice;
        const product = { ...p, originalPrice: original && original > p.price ? original : null };
        if (product.originalPrice && !product.discount) {
            product.discount = Math.round((1 - product.price / product.originalPrice) * 100);
        }
        if (!product.specifications && product.attributesJson) {
            try {
                const attrs = JSON.parse(product.attributesJson);
                if (attrs && typeof attrs === 'object' && !Array.isArray(attrs)) product.specifications = attrs;
            } catch { /* attributes are optional free-form JSON */ }
        }
        return product;
    }

    /** Stock is only enforced when the API actually reports it. */
    get hasStockInfo(): boolean {
        return typeof this.product?.stock === 'number';
    }

    get outOfStock(): boolean {
        return this.hasStockInfo && this.product.stock <= 0;
    }

    get maxQuantity(): number {
        return this.hasStockInfo ? Math.max(1, Math.min(this.product.stock, 99)) : 99;
    }

    /** Mirrors the backend rule: 1 NexPoint per 1.000 ₫ when the order is delivered. */
    get estimatedPoints(): number {
        return Math.floor((Number(this.product?.price) || 0) * this.quantity / 1000);
    }

    get isWished(): boolean {
        return !!this.product && this.wishlistService.has(Number(this.product.id));
    }

    toggleWishlist() {
        if (!this.isLoggedIn) {
            this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
            return;
        }
        this.wishlistService.toggle(Number(this.product.id)).subscribe({
            next: wished => this.toastr.success(wished ? 'Đã thêm vào yêu thích' : 'Đã bỏ khỏi yêu thích'),
            error: () => this.toastr.error('Không thể cập nhật yêu thích. Vui lòng thử lại.')
        });
    }

    scrollToReviews() {
        document.getElementById('reviews')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    onQuantityChange(value: number) {
        const n = Math.floor(Number(value));
        this.quantity = Number.isFinite(n) ? Math.min(Math.max(n, 1), this.maxQuantity) : 1;
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
        if (this.quantity < this.maxQuantity) {
            this.quantity++;
        }
    }

    decreaseQuantity() {
        if (this.quantity > 1) {
            this.quantity--;
        }
    }

    addToCart(onAdded?: () => void) {
        if (!this.product || this.addingToCart || this.outOfStock) return;

        this.addingToCart = true;

        this.cartService.addItem(this.product.id, this.quantity).subscribe({
            next: () => {
                this.addingToCart = false;
                if (onAdded) {
                    onAdded();
                } else {
                    this.toastr.success('Đã thêm vào giỏ hàng!');
                }
            },
            error: (err) => {
                this.addingToCart = false;
                this.toastr.error(err?.message || 'Không thể thêm vào giỏ hàng. Vui lòng thử lại.');
            }
        });
    }

    /** Navigate only once the item is really in the cart (the old 500 ms timer raced the request). */
    buyNow() {
        this.addToCart(() => this.router.navigate(['/cart']));
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
                this.reviews = res?.content || [];
                this.totalReviews = res?.totalElements ?? this.reviews.length;
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

    /** Always 5 stars in total: e.g. 4.8 → 5 filled, 4.2 → 4 filled + 1 empty. */
    filledStars(rating: number): any[] {
        return this.getStarArray(Math.min(5, Math.max(0, Math.round(rating || 0))));
    }

    emptyStars(rating: number): any[] {
        return this.getStarArray(5 - Math.min(5, Math.max(0, Math.round(rating || 0))));
    }
}
