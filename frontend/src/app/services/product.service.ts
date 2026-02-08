import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Product {
    id: number;
    name: string;
    description?: string;
    price: number;
    originalPrice?: number;
    discount?: number;
    imageUrl?: string;
    categoryId?: number;
    categoryName?: string;
    brand?: string;
    rating?: number;
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
        }

        return this.http.get<ProductListResponse>(this.apiUrl, { params: httpParams });
    }

    /**
     * Search products
     */
    searchProducts(keyword: string, page: number = 0, size: number = 20): Observable<ProductListResponse> {
        const params = new HttpParams()
            .set('keyword', keyword)
            .set('page', page.toString())
            .set('size', size.toString());

        return this.http.get<ProductListResponse>(`${this.apiUrl}/search`, { params });
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
}
