import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface SellerApplication {
    id?: number;
    userId?: number;
    shopName: string;
    shopDescription: string;
    businessLicense: string;
    taxCode: string;
    phoneNumber: string;
    address: string;
    categoryIds: number[];
    status?: string; // 'PENDING' | 'APPROVED' | 'REJECTED'
    createdAt?: string;
    updatedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class SellerApplicationService {
    private apiUrl = `${environment.apiBaseUrl}/seller-applications`;

    constructor(private http: HttpClient) { }

    /**
     * Get my seller application
     * Returns 404 if no application exists
     */
    getMyApplication(): Observable<SellerApplication | null> {
        return this.http.get<SellerApplication>(`${this.apiUrl}/my-application`);
    }

    /**
     * Submit seller application
     */
    submitApplication(application: Omit<SellerApplication, 'id' | 'userId' | 'status'>): Observable<SellerApplication> {
        return this.http.post<SellerApplication>(this.apiUrl, application);
    }

    /**
     * Get application by ID (admin)
     */
    getApplication(id: number): Observable<SellerApplication> {
        return this.http.get<SellerApplication>(`${this.apiUrl}/${id}`);
    }

    /**
     * Get all applications (admin)
     */
    getAllApplications(status?: string): Observable<SellerApplication[]> {
        const url = status ? `${this.apiUrl}?status=${status}` : this.apiUrl;
        return this.http.get<SellerApplication[]>(url);
    }

    /**
     * Approve application (admin)
     */
    approveApplication(id: number): Observable<SellerApplication> {
        return this.http.put<SellerApplication>(`${this.apiUrl}/${id}/approve`, {});
    }

    /**
     * Reject application (admin)
     */
    rejectApplication(id: number, reason?: string): Observable<SellerApplication> {
        return this.http.put<SellerApplication>(`${this.apiUrl}/${id}/reject`, { reason });
    }
}
