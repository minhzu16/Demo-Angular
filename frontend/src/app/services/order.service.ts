import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface OrderItem {
    productId: number;
    productName?: string;
    quantity: number;
    price: number;
    imageUrl?: string;
}

export interface Order {
    id: number;
    userId: number;
    status: string;
    totalAmount: number;
    shippingAddress?: string;
    paymentMethod?: string;
    items: OrderItem[];
    createdAt: string;
    updatedAt?: string;
}

export interface CreateOrderRequest {
    userId: number;
    shippingAddress: string;
    paymentMethod: string;
    items: OrderItem[];
    voucherCode?: string;
    note?: string;
}

export interface OrderListResponse {
    content: Order[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
}

export interface OrderStats {
    totalOrders: number;
    completedOrders: number;
    pendingOrders: number;
    cancelledOrders: number;
}

@Injectable({ providedIn: 'root' })
export class OrderService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiBaseUrl}/orders`;

    private getAuthHeaders(userId?: number): HttpHeaders {
        let headers = new HttpHeaders({ 'Content-Type': 'application/json' });

        // Get token from localStorage
        const token = localStorage.getItem('access_token');
        if (token) {
            headers = headers.set('Authorization', `Bearer ${token}`);
        }

        if (userId) {
            headers = headers.set('X-User-Id', userId.toString());
        }

        return headers;
    }

    /**
     * Create a new order
     */
    createOrder(request: CreateOrderRequest, userId?: number): Observable<Order> {
        const headers = this.getAuthHeaders(userId || request.userId);
        return this.http.post<Order>(this.apiUrl, request, { headers });
    }

    /**
     * Get all orders
     */
    getAllOrders(): Observable<Order[]> {
        const headers = this.getAuthHeaders();
        return this.http.get<Order[]>(this.apiUrl, { headers });
    }

    /**
     * Get orders by user ID
     */
    getOrdersByUser(userId: number): Observable<Order[]> {
        const headers = this.getAuthHeaders(userId);
        return this.http.get<Order[]>(`${this.apiUrl}/user/${userId}`, { headers });
    }

    /**
     * Get my orders with pagination and filters
     */
    getMyOrders(
        userId: number,
        page: number = 0,
        size: number = 10,
        status?: string,
        startDate?: string,
        endDate?: string
    ): Observable<OrderListResponse> {
        const headers = this.getAuthHeaders(userId);
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        if (status) params = params.set('status', status);
        if (startDate) params = params.set('startDate', startDate);
        if (endDate) params = params.set('endDate', endDate);

        return this.http.get<OrderListResponse>(`${this.apiUrl}/my-orders`, { headers, params });
    }

    /**
     * Get order by ID
     */
    getOrder(orderId: number): Observable<Order> {
        const headers = this.getAuthHeaders();
        return this.http.get<Order>(`${this.apiUrl}/${orderId}`, { headers });
    }

    /**
     * Cancel order
     */
    cancelOrder(orderId: number, userId?: number): Observable<Order> {
        const headers = this.getAuthHeaders(userId);
        return this.http.post<Order>(`${this.apiUrl}/${orderId}/cancel`, {}, { headers });
    }

    /**
     * Confirm order received
     */
    confirmReceived(orderId: number, userId: number): Observable<Order> {
        const headers = this.getAuthHeaders(userId);
        return this.http.put<Order>(`${this.apiUrl}/${orderId}/confirm-received`, {}, { headers });
    }

    /**
     * Request return/refund
     */
    requestReturn(orderId: number, userId: number): Observable<Order> {
        const headers = this.getAuthHeaders(userId);
        return this.http.post<Order>(`${this.apiUrl}/${orderId}/return-request`, {}, { headers });
    }

    /**
     * Get order statistics for user
     */
    getUserOrderStats(userId: number): Observable<OrderStats> {
        const headers = this.getAuthHeaders(userId);
        return this.http.get<OrderStats>(`${this.apiUrl}/stats/user/${userId}`, { headers });
    }

    /**
     * Get my order statistics
     */
    getMyOrderStats(userId: number): Observable<OrderStats> {
        const headers = this.getAuthHeaders(userId);
        return this.http.get<OrderStats>(`${this.apiUrl}/stats`, { headers });
    }

    /**
     * Get order status history
     */
    getOrderStatusHistory(orderId: number): Observable<any[]> {
        const headers = this.getAuthHeaders();
        return this.http.get<any[]>(`${this.apiUrl}/${orderId}/status-history`, { headers });
    }

    /**
     * Create payment session for order
     */
    createPaymentSession(orderId: number): Observable<{ sessionId: string; url: string }> {
        const headers = this.getAuthHeaders();
        return this.http.post<any>(`${this.apiUrl}/${orderId}/payment-session`, {}, { headers });
    }

    /**
     * Get orders for shop (seller dashboard)
     */
    getShopOrders(
        shopId: number,
        page: number = 0,
        size: number = 10
    ): Observable<OrderListResponse> {
        const headers = this.getAuthHeaders();
        const params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        return this.http.get<OrderListResponse>(`${this.apiUrl}/shop/${shopId}`, { headers, params });
    }

    /**
     * Get shop order statistics
     */
    getShopOrderStats(shopId: number): Observable<any> {
        const headers = this.getAuthHeaders();
        return this.http.get<any>(`${this.apiUrl}/shop/${shopId}/statistics`, { headers });
    }

    /**
     * Get product sold count
     */
    getProductSoldCount(productId: number): Observable<number> {
        return this.http.get<number>(`${this.apiUrl}/product/${productId}/sold-count`);
    }

    /**
     * Alias for getShopOrders - for backward compatibility
     */
    getOrdersByShop(shopId: number, options: { page: number; size: number }): Observable<OrderListResponse> {
        return this.getShopOrders(shopId, options.page, options.size);
    }

    /**
     * Alias for getShopOrderStats - for backward compatibility
     */
    getOrderStatistics(shopId: number): Observable<any> {
        return this.getShopOrderStats(shopId);
    }

    /**
     * Update order status (Seller/Admin)
     */
    updateOrderStatus(orderId: number, status: string): Observable<Order> {
        const headers = this.getAuthHeaders();
        return this.http.put<Order>(`${this.apiUrl}/${orderId}/status`, { status }, { headers });
    }
}
