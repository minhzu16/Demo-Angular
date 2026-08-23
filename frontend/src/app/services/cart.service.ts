import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CartItem {
    productId: number;
    productName?: string;
    quantity: number;
    price: number;
    imageUrl?: string;
    totalPrice?: number;
    selected?: boolean;
    maxStock?: number;
}

export interface Cart {
    id?: number;
    userId?: number;
    sessionId?: string;
    items: CartItem[];
    totalItems: number;
    totalAmount: number;
    createdAt?: string;
    updatedAt?: string;
}

export interface AddItemRequest {
    productId: number;
    quantity: number;
    userId?: number;
}

@Injectable({ providedIn: 'root' })
export class CartService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiBaseUrl}/cart`;

    /**
     * Get cart for current user or session
     */
    getCart(userId?: number, sessionId?: string): Observable<Cart> {
        let url = this.apiUrl;
        const params: any = {};

        if (userId) {
            params.userId = userId.toString();
        }
        if (sessionId) {
            params.sessionId = sessionId;
        }

        return this.http.get<Cart>(url, { params });
    }

    /**
     * Get cart by user ID
     */
    getCartByUserId(userId: number): Observable<Cart> {
        return this.http.get<Cart>(`${this.apiUrl}/user/${userId}`);
    }

    /**
     * Get cart items count
     */
    getCartCount(userId?: number, sessionId?: string): Observable<number> {
        const params: any = {};
        if (userId) params.userId = userId.toString();
        if (sessionId) params.sessionId = sessionId;

        return this.http.get<number>(`${this.apiUrl}/count`, { params });
    }

    /**
     * Add item to cart
     */
    addItem(productId: number, quantity: number = 1, userId?: number): Observable<Cart> {
        let headers = new HttpHeaders();
        if (userId) {
            headers = headers.set('X-User-Id', userId.toString());
        }

        const body: AddItemRequest = {
            productId,
            quantity,
            userId
        };

        return this.http.post<Cart>(this.apiUrl, body, { headers });
    }

    /**
     * Update item quantity
     */
    updateQuantity(productId: number, quantity: number, userId?: number, sessionId?: string): Observable<Cart> {
        const params: any = {};
        if (userId) params.userId = userId.toString();
        if (sessionId) params.sessionId = sessionId;

        const body = { quantity };

        return this.http.put<Cart>(`${this.apiUrl}/items/${productId}`, body, { params });
    }

    /**
     * Remove item from cart
     */
    removeItem(productId: number, userId?: number, sessionId?: string): Observable<Cart> {
        const params: any = {};
        if (userId) params.userId = userId.toString();
        if (sessionId) params.sessionId = sessionId;

        return this.http.delete<Cart>(`${this.apiUrl}/items/${productId}`, { params });
    }

    /**
     * Get cart summary
     */
    getCartSummary(userId?: number, sessionId?: string): Observable<{ totalItems: number; totalAmount: number; itemCount: number }> {
        let headers = new HttpHeaders();
        if (userId) {
            headers = headers.set('X-User-Id', userId.toString());
        }

        const params: any = {};
        if (sessionId) params.sessionId = sessionId;

        return this.http.get<any>(`${this.apiUrl}/summary`, { headers, params });
    }

    /**
     * Validate cart
     */
    validateCart(userId?: number, sessionId?: string): Observable<{ valid: boolean; totalItems: number; errors: any[] }> {
        let headers = new HttpHeaders();
        if (userId) {
            headers = headers.set('X-User-Id', userId.toString());
        }

        const params: any = {};
        if (sessionId) params.sessionId = sessionId;

        return this.http.get<any>(`${this.apiUrl}/validate`, { headers, params });
    }

    /**
     * Clear cart
     */
    clearCart(userId?: number, sessionId?: string): Observable<void> {
        let headers = new HttpHeaders();
        if (userId) {
            headers = headers.set('X-User-Id', userId.toString());
        }

        const params: any = {};
        if (sessionId) params.sessionId = sessionId;

        return this.http.delete<void>(`${this.apiUrl}/clear`, { headers, params });
    }

    /**
     * Merge guest cart to user cart
     */
    mergeCart(userId: number, sessionId: string): Observable<Cart> {
        const params = {
            userId: userId.toString(),
            sessionId: sessionId
        };

        return this.http.post<Cart>(`${this.apiUrl}/merge`, null, { params });
    }

    /**
     * Create session for guest user
     */
    createSession(): Observable<{ sessionId: string }> {
        return this.http.post<{ sessionId: string }>(`${this.apiUrl}/session`, {});
    }
}
