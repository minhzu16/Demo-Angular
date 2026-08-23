import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CreatePaymentRequest {
    orderId: number;
    amount: number;
    currency: string; // 'VND', 'USD'
    paymentMethod: string; // 'STRIPE', 'MOMO', 'COD'
}

export interface PaymentDto {
    id: number;
    orderId: number;
    amount: number;
    currency: string;
    paymentMethod: string;
    paymentStatus: string; // 'PENDING', 'SUCCESS', 'FAILED', 'CANCELLED'
    transactionId: string;
    paymentIntentId?: string;
    redirectUrl?: string;
    createdAt: string;
    updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiBaseUrl}/payments`;

    /**
     * Create payment task
     */
    createPayment(request: CreatePaymentRequest): Observable<PaymentDto> {
        return this.http.post<PaymentDto>(`${this.apiUrl}/create`, request);
    }

    /**
     * Get payment info for an order
     */
    getPaymentInfo(orderId: number): Observable<PaymentDto> {
        return this.http.get<PaymentDto>(`${this.apiUrl}/order/${orderId}/info`);
    }

    /**
     * Confirm/Update payment by intent ID and status
     */
    confirmPayment(intentId: string, status: string): Observable<PaymentDto> {
        return this.http.post<PaymentDto>(`${this.apiUrl}/confirm/${intentId}/${status}`, {});
    }

    /**
     * Update payment status of an order
     */
    updatePaymentStatus(orderId: number, status: string): Observable<PaymentDto> {
        const params = new HttpParams().set('status', status);
        return this.http.put<PaymentDto>(`${this.apiUrl}/order/${orderId}/status`, {}, { params });
    }
}
