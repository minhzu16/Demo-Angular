import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Voucher {
    id: number;
    code: string;
    description?: string;
    type: 'FIXED' | 'PERCENTAGE';
    value: number;
    minOrderValue?: number;
    maxDiscount?: number;
    validUntil?: string;
}

export interface VoucherValidationResponse {
    valid: boolean;
    voucher?: Voucher;
    message: string;
    discountAmount?: number;
}

@Injectable({ providedIn: 'root' })
export class VoucherService {
    private http = inject(HttpClient);
    // Voucher controller uses /api (no v1) for its validate/valid endpoints
    private apiUrl = `${environment.apiBaseUrl.replace('/v1', '')}/vouchers`;

    /**
     * Validate a voucher code against an order total
     * Calls: POST /api/vouchers/validate
     */
    validateVoucher(code: string, orderTotal: number): Observable<VoucherValidationResponse> {
        return this.http.post<VoucherValidationResponse>(`${this.apiUrl}/validate`, {
            code: code,
            orderTotal: orderTotal
        });
    }

    /**
     * Get list of valid vouchers
     * Calls: GET /api/vouchers/valid
     */
    getValidVouchers(): Observable<Voucher[]> {
        return this.http.get<Voucher[]>(`${this.apiUrl}/valid`);
    }
}
