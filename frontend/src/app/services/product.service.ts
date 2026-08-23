import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

export interface Product {
    id: number;
    name: string;
    description?: string;
    price: number;
    originalPrice?: number;
    discount?: number;
    imageUrl?: string;
    thumbnailUrl?: string;
    categoryId?: number;
    categoryName?: string;
    brand?: string;
    averageRating?: number;
    reviewCount?: number;
    soldCount?: number;
    stock?: number;
}

export interface ProductListResponse {
    content: Product[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
}

@Injectable({ providedIn: 'root' })
export class ProductService {
    private http = inject(HttpClient);
    private authService = inject(AuthService);
    private apiUrl = `${environment.apiBaseUrl}/products`;

    /**
     * Get list of products with pagination and filters
     */
    getProducts(params?: {
        keyword?: string;
        category?: number;
        brand?: string;
        minPrice?: number;
        maxPrice?: number;
        sort?: string;
        page?: number;
        size?: number;
        sellerId?: number;
    }): Observable<ProductListResponse> {
        let httpParams = new HttpParams();

        if (params) {
            if (params.keyword) httpParams = httpParams.set('keyword', params.keyword);
            if (params.category) httpParams = httpParams.set('category', params.category.toString());
            if (params.brand) httpParams = httpParams.set('brand', params.brand);
            if (params.minPrice) httpParams = httpParams.set('minPrice', params.minPrice.toString());
            if (params.maxPrice) httpParams = httpParams.set('maxPrice', params.maxPrice.toString());
            if (params.sort) httpParams = httpParams.set('sort', params.sort);
            if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
            if (params.size) httpParams = httpParams.set('size', params.size.toString());
            if (params.sellerId) httpParams = httpParams.set('sellerId', params.sellerId.toString());
        }

        return this.http.get<ProductListResponse>(this.apiUrl, { params: httpParams });
    }

    /**
     * Search products using Advanced Elasticsearch Facets
     */
    searchProducts(params: {
        keyword?: string;
        category?: number;
        brand?: string;
        minPrice?: number;
        maxPrice?: number;
        sort?: string;
        page?: number;
        size?: number;
    }): Observable<ProductListResponse> {
        let httpParams = new HttpParams();

        if (params) {
            // Map keyword to 'q' which is used by ProductSearchController
            if (params.keyword) httpParams = httpParams.set('q', params.keyword);
            if (params.category) httpParams = httpParams.set('category', params.category.toString());
            if (params.brand) httpParams = httpParams.set('brand', params.brand);
            if (params.minPrice) httpParams = httpParams.set('minPrice', params.minPrice.toString());
            if (params.maxPrice) httpParams = httpParams.set('maxPrice', params.maxPrice.toString());
            if (params.sort) httpParams = httpParams.set('sort', params.sort);
            if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
            if (params.size) httpParams = httpParams.set('size', params.size.toString());
        }

        return this.http.get<ProductListResponse>(`${this.apiUrl}/search`, { params: httpParams });
    }

    /**
     * Get product detail by ID
     */
    getProductDetail(id: number): Observable<Product> {
        return this.http.get<Product>(`${this.apiUrl}/${id}`);
    }

    /**
     * Get product variants
     */
    getProductVariants(productId: number): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/${productId}/variants`);
    }

    /**
     * Get products by category
     */
    getProductsByCategory(categoryId: number, page: number = 0, size: number = 20): Observable<ProductListResponse> {
        return this.getProducts({ category: categoryId, page, size });
    }

    /**
     * Get featured products
     */
    getFeaturedProducts(size: number = 10): Observable<ProductListResponse> {
        return this.getProducts({ size, sort: 'rating_desc' });
    }

    /**
     * Get best selling products
     */
    getBestSellingProducts(size: number = 10): Observable<ProductListResponse> {
        return this.getProducts({ size, sort: 'sold_desc' });
    }

    /**
     * Get active flash sale
     */
    getActiveFlashSale(): Observable<any> {
        return this.http.get<any>(`${environment.apiBaseUrl}/flash-sales/active`);
    }

    /**
     * Get all flash sales (Admin)
     */
    getFlashSales(status?: string): Observable<any> {
        let params = new HttpParams();
        if (status) params = params.set('status', status);
        return this.http.get<any>(`${environment.apiBaseUrl}/flash-sales`, { params });
    }

    /**
     * Create new flash sale (Admin)
     */
    createFlashSale(data: any): Observable<any> {
        return this.http.post<any>(`${environment.apiBaseUrl}/flash-sales`, data);
    }

    /**
     * Update flash sale (Admin)
     */
    updateFlashSale(id: number, data: any): Observable<any> {
        return this.http.put<any>(`${environment.apiBaseUrl}/flash-sales/${id}`, data);
    }

    /**
     * Delete flash sale (Admin)
     */
    deleteFlashSale(id: number): Observable<any> {
        return this.http.delete<any>(`${environment.apiBaseUrl}/flash-sales/${id}`);
    }

    /**
     * End flash sale early (Admin)
     */
    endFlashSale(id: number): Observable<any> {
        return this.http.post<any>(`${environment.apiBaseUrl}/flash-sales/${id}/end`, {});
    }

    /**
     * Get products in flash sale (Admin)
     */
    getFlashSaleProducts(id: number): Observable<any> {
        return this.http.get<any>(`${environment.apiBaseUrl}/flash-sales/${id}/products`);
    }

    /**
     * Add product to flash sale (Admin)
     */
    addProductToFlashSale(id: number, data: any): Observable<any> {
        return this.http.post<any>(`${environment.apiBaseUrl}/flash-sales/${id}/products`, data);
    }
    /**
     * Remove product from flash sale (Admin)
     */
    removeProductFromFlashSale(id: number, productId: number): Observable<any> {
        return this.http.delete<any>(`${environment.apiBaseUrl}/flash-sales/${id}/products/${productId}`, { headers: this.getAuthHeaders() });
    }

    // --- Seller Product Management ---

    createProduct(product: any): Observable<any> {
        return this.http.post<any>(this.apiUrl, product, { headers: this.getAuthHeaders() });
    }

    updateProduct(id: number, product: any): Observable<any> {
        return this.http.put<any>(`${this.apiUrl}/${id}`, product, { headers: this.getAuthHeaders() });
    }

    deleteProduct(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`, { headers: this.getAuthHeaders() });
    }

    getSellerProducts(sellerId: number, page: number = 0, size: number = 20): Observable<ProductListResponse> {
        return this.getProducts({ sellerId, page, size });
    }

    private getAuthHeaders(): HttpHeaders {
        const token = this.authService.getToken();
        const user = this.authService.getUser();
        let headers = new HttpHeaders({ 'Content-Type': 'application/json' });
        if (token) {
            headers = headers.set('Authorization', `Bearer ${token}`);
        }
        if (user && user.id) {
            headers = headers.set('X-User-Id', user.id.toString());
        }
        return headers;
    }
}
