import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface Shop {
    id: number;
    name: string;
    description?: string;
    userId: number;
}

@Injectable({ providedIn: 'root' })
export class ShopService {
    private apiUrl = `${environment.apiBaseUrl}/shops`;

    constructor(private http: HttpClient) { }

    /**
     * Get my shop (for seller)
     * Returns 404 if no shop exists
     */
    getMyShop(): Observable<Shop> {
        return this.http.get<Shop>(`${this.apiUrl}/my-shop`);
    }

    /**
     * Get shop by ID
     */
    getShop(id: number): Observable<Shop> {
        return this.http.get<Shop>(`${this.apiUrl}/${id}`);
    }

    /**
     * Create a new shop
     */
    createShop(shop: Partial<Shop>): Observable<Shop> {
        return this.http.post<Shop>(this.apiUrl, shop);
    }

    /**
     * Update shop
     */
    updateShop(id: number, shop: Partial<Shop>): Observable<Shop> {
        return this.http.put<Shop>(`${this.apiUrl}/${id}`, shop);
    }
}
