import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SalesOverview {
    totalRevenue: number;
    totalOrders: number;
    averageOrderValue: number;
    period: {
        start: string;
        end: string;
    };
}

export interface SalesRangeResponse {
    sales: any[];
    summary: {
        totalRevenue: number;
        totalOrders: number;
        days: number;
    };
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiBaseUrl}/analytics/sales`;

    /**
     * Get sales overview
     */
    getSalesOverview(startDate: string, endDate: string, shopId?: number): Observable<SalesOverview> {
        let params = new HttpParams()
            .set('startDate', startDate)
            .set('endDate', endDate);
        if (shopId) params = params.set('shopId', shopId.toString());
        return this.http.get<SalesOverview>(`${this.apiUrl}/overview`, { params });
    }

    /**
     * Get sales by range
     */
    getSalesByRange(startDate: string, endDate: string, shopId?: number): Observable<SalesRangeResponse> {
        let params = new HttpParams()
            .set('startDate', startDate)
            .set('endDate', endDate);
        if (shopId) params = params.set('shopId', shopId.toString());
        return this.http.get<SalesRangeResponse>(`${this.apiUrl}/range`, { params });
    }

    /**
     * Get dashboard summary for a shop
     */
    getDashboard(shopId: number): Observable<any> {
        const params = new HttpParams().set('shopId', shopId.toString());
        return this.http.get<any>(`${this.apiUrl}/dashboard`, { params });
    }

    /**
     * Get top products for a shop
     */
    getTopProducts(shopId: number, limit: number = 10, days: number = 30): Observable<any[]> {
        const params = new HttpParams()
            .set('shopId', shopId.toString())
            .set('limit', limit.toString())
            .set('days', days.toString());
        return this.http.get<any[]>(`${this.apiUrl}/top-products`, { params });
    }

    /**
     * Get revenue by period for charts
     */
    getRevenue(shopId: number, start: string, end: string, period: string = 'daily'): Observable<any[]> {
        const params = new HttpParams()
            .set('shopId', shopId.toString())
            .set('start', start)
            .set('end', end)
            .set('period', period);
        return this.http.get<any[]>(`${this.apiUrl}/revenue`, { params });
    }

    /**
     * Get product recommendations for user
     */
    getRecommendations(userId: number): Observable<any[]> {
        return this.http.get<any[]>(`${environment.apiBaseUrl}/analytics/recommendations/${userId}`);
    }

    /**
     * Get trending products
     */
    getTrending(): Observable<any[]> {
        return this.http.get<any[]>(`${environment.apiBaseUrl}/analytics/recommendations/trending`);
    }
}
