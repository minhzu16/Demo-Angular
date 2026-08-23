import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface ProductStats {
    totalProducts?: number;
    lowStockProducts?: number;
    outOfStockProducts?: number;
    totalValue?: number;
}

export interface StockItem {
    id: number;
    productId: number;
    productName: string;
    quantity: number;
    minStock?: number;
    maxStock?: number;
    warehouseId?: number;
}

@Injectable({ providedIn: 'root' })
export class WarehouseService {
    private apiUrl = `${environment.apiBaseUrl}/warehouse`;

    constructor(private http: HttpClient) { }

    /**
     * Get stock statistics for a shop
     */
    getShopProductStats(shopId: number): Observable<ProductStats> {
        return this.http.get<ProductStats>(`${this.apiUrl}/shops/${shopId}/stats`);
    }

    /**
     * Get stock items for a shop
     */
    getShopStock(shopId: number): Observable<StockItem[]> {
        return this.http.get<StockItem[]>(`${this.apiUrl}/shops/${shopId}/stock`);
    }

    /**
     * Get stock for a specific product
     */
    getProductStock(productId: number): Observable<StockItem> {
        return this.http.get<StockItem>(`${this.apiUrl}/products/${productId}`);
    }

    /**
     * Update stock quantity
     */
    updateStock(productId: number, quantity: number): Observable<StockItem> {
        return this.http.put<StockItem>(`${this.apiUrl}/products/${productId}`, { quantity });
    }
}
