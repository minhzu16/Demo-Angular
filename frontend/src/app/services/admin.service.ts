import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface AdminUser {
    id: number;
    username: string;
    email: string;
    fullName: string;
    role: string;
    status: string;
    loyaltyTier: string;
    loyaltyPoints: number;
    createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
    private apiUrl = `${environment.apiBaseUrl}/admin`;

    constructor(private http: HttpClient) { }

    getUsers(page: number = 0, size: number = 10): Observable<any> {
        return this.http.get<any>(`${this.apiUrl}/users?page=${page}&size=${size}`);
    }

    updateUserStatus(userId: number, status: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/users/${userId}/status?status=${status}`, {});
    }
}
