import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService, ProductListResponse } from '../../services/product.service';
import { CompareService } from '../../services/compare.service';
import { Subject, Subscription } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

@Component({
    selector: 'app-products',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule],
    templateUrl: './products.component.html',
    styleUrls: ['./products.component.scss']
})
export class ProductsComponent implements OnInit, OnDestroy {
    private productService = inject(ProductService);
    private router = inject(Router);
    private compareService = inject(CompareService);

    filteredProducts: any[] = [];
    loading = false;
    error = '';

    // Filters
    searchQuery = '';
    selectedCategoryStr = '';
    selectedBrand = '';
    minPrice = 0;
    maxPrice = 50000000;
    sortBy = 'newest';

    categories = [
        { id: 1, name: 'Điện tử' },
        { id: 2, name: 'Thời trang' },
        { id: 3, name: 'Nhà bếp' },
        { id: 4, name: 'Sách' },
        { id: 5, name: 'Thể thao' },
        { id: 6, name: 'Mỹ phẩm' },
        { id: 7, name: 'Đồ chơi' },
        { id: 8, name: 'Ô tô' }
    ];
    brands = ['Apple', 'Samsung', 'Xiaomi', 'Sony', 'LG', 'Dell', 'HP', 'Asus', 'Nike', 'Adidas'];

    // Pagination
    currentPage = 1;
    pageSize = 12;
    totalPages = 1;

    // RxJS for debounce
    private searchSubject = new Subject<void>();
    private searchSubscription!: Subscription;

    ngOnInit() {
        this.loadProducts();

        // Setup debounce for filter changes (500ms delay)
        this.searchSubscription = this.searchSubject.pipe(
            debounceTime(500)
        ).subscribe(() => {
            this.currentPage = 1; // Reset to page 1 on search
            this.loadProducts();
        });
    }

    ngOnDestroy() {
        if (this.searchSubscription) {
            this.searchSubscription.unsubscribe();
        }
    }

    loadProducts() {
        this.loading = true;
        this.error = '';

        // Find Category ID
        const activeCategory = this.categories.find(c => c.name === this.selectedCategoryStr);

        this.productService.searchProducts({
            keyword: this.searchQuery,
            category: activeCategory ? activeCategory.id : undefined,
            brand: this.selectedBrand || undefined,
            minPrice: this.minPrice > 0 ? this.minPrice : undefined,
            maxPrice: this.maxPrice < 50000000 ? this.maxPrice : undefined,
            sort: this.mapSortParameter(this.sortBy),
            page: this.currentPage - 1,
            size: this.pageSize
        }).subscribe({
            next: (response: ProductListResponse) => {
                this.filteredProducts = response.content || [];
                this.totalPages = response.totalPages || Math.ceil(this.filteredProducts.length / this.pageSize);
                this.loading = false;
            },
            error: (err) => {
                this.error = 'Không thể kết nối đến máy chủ tìm kiếm. Vui lòng thử lại sau.';
                this.loading = false;
                this.filteredProducts = [];
            }
        });
    }

    applyFilters() {
        // Debounce the API call
        this.searchSubject.next();
    }

    clearFilters() {
        this.searchQuery = '';
        this.selectedCategoryStr = '';
        this.selectedBrand = '';
        this.minPrice = 0;
        this.maxPrice = 50000000;
        this.sortBy = 'newest';
        this.currentPage = 1;
        this.loadProducts();
    }

    viewProduct(productId: number) {
        this.router.navigate(['/products', productId]);
    }

    changePage(page: number) {
        if (page >= 1 && page <= this.totalPages) {
            this.currentPage = page;
            this.loadProducts();
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }
    }

    formatPrice(price: number): string {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
    }


    private mapSortParameter(sortKey: string): string {
        switch (sortKey) {
            case 'price-asc': return 'price,asc';
            case 'price-desc': return 'price,desc';
            case 'name': return 'name,asc';
            case 'rating': return 'rating,desc';
            case 'popular': return 'soldCount,desc';
            default: return 'id,desc';
        }
    }

    toggleWishlist(product: any): void {
        product.wished = !product.wished;
    }

    addToCompare(product: any): void {
        this.compareService.addToCompare(product);
    }

    setQuickPrice(min: number, max: number): void {
        this.minPrice = min;
        this.maxPrice = max > 0 ? max : 50000000;
        this.applyFilters();
    }

    getStarArray(n: number): any[] {
        return Array(Math.max(0, Math.floor(n))).fill(0);
    }

    getPageArray(): number[] {
        const pages: number[] = [];
        const start = Math.max(1, this.currentPage - 2);
        const end   = Math.min(this.totalPages, this.currentPage + 2);
        for (let i = start; i <= end; i++) pages.push(i);
        return pages;
    }
}
