import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, tap, map } from 'rxjs/operators';
import { Observable, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

interface LoginRequest { usernameOrEmail: string; password: string; }
interface RegisterRequest { username: string; password: string; firstName: string; lastName: string; company?: string; }
interface LoginResponse { accessToken: string; user: UserProfile; }
export interface UserProfile {
  id: number;
  username: string;
  fullName: string;
  age?: number;
  email?: string;
  phoneNumber?: string;
  address?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  workplace?: string;
  role?: string;
  loyaltyPoints?: number;
  loyaltyTier?: string;
}

export interface UserAddress {
  id?: number;
  receiverName: string;
  phoneNumber: string;
  address: string;
  isDefault?: boolean;
}

export interface LoyaltyTransaction {
  id: number;
  pointsChange: number;
  balanceAfter: number;
  reason: string;
  referenceId: string;
  createdAt: string;
}

const TOKEN_KEY = 'access_token';
const USER_KEY = 'user_profile';
const API_BASE = environment.apiBaseUrl;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  // Address Management
  getAddresses(): Observable<UserAddress[]> {
    return this.http.get<UserAddress[]>(`${API_BASE}/users/addresses`, { headers: this.authHeaders() });
  }

  addAddress(address: UserAddress): Observable<UserAddress> {
    return this.http.post<UserAddress>(`${API_BASE}/users/addresses`, address, { headers: this.authHeaders() });
  }

  updateAddress(id: number, address: UserAddress): Observable<UserAddress> {
    return this.http.put<UserAddress>(`${API_BASE}/users/addresses/${id}`, address, { headers: this.authHeaders() });
  }

  deleteAddress(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE}/users/addresses/${id}`, { headers: this.authHeaders() });
  }

  setDefaultAddress(id: number): Observable<void> {
    return this.http.post<void>(`${API_BASE}/users/addresses/${id}/default`, {}, { headers: this.authHeaders() });
  }

  getLoyaltyHistory(page: number = 0, size: number = 10): Observable<any> {
    return this.http.get<any>(`${API_BASE}/users/me/loyalty-history?page=${page}&size=${size}`, { headers: this.authHeaders() });
  }

  private authHeaders(): HttpHeaders {
    const token = this.getToken();
    const user = this.getUser();
    let headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    if (user && user.id) {
      headers = headers.set('X-User-Id', user.id.toString());
      if (user.username) {
        headers = headers.set('X-Username', user.username);
      }
    }
    return headers;
  }

  login(body: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API_BASE}/auth/login`, body)
      .pipe(
        tap(response => {
          if (response.accessToken) {
            this.saveToken(response.accessToken);
            if (response.user) {
              this.saveUser(response.user);
            }
          }
        }),
        catchError((error: HttpErrorResponse) => throwError(() => error))
      );
  }

  register(body: RegisterRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API_BASE}/auth/register`, body)
      .pipe(
        tap(response => {
          if (response.accessToken) {
            this.saveToken(response.accessToken);
            if (response.user) {
              this.saveUser(response.user);
            }
          }
        }),
        catchError((error: HttpErrorResponse) => throwError(() => error))
      );
  }

  saveToken(token: string) {
    localStorage.setItem(TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  saveUser(user: UserProfile) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  getUser(): UserProfile | null {
    const userStr = localStorage.getItem(USER_KEY);
    return userStr ? JSON.parse(userStr) : null;
  }

  isAuthenticated(): boolean {
    return this.isLoggedIn();
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) return false;

    try {
      // JWT is Base64Url encoded. We must convert it to standard Base64 before using atob().
      let base64Url = token.split('.')[1];
      let base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');

      const payload = JSON.parse(atob(base64));
      const currentTime = Date.now() / 1000;
      if (payload.exp && payload.exp < currentTime) {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        return false;
      }
      return true;
    } catch (error) {
      console.error('JWT Parsing error:', error);
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      return false;
    }
  }

  logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.router.navigateByUrl('/login');
  }

  getProfile(): Observable<UserProfile> {
    return this.http.get<any>(`${API_BASE}/users/me`, { headers: this.authHeaders() }).pipe(
      map(userInfo => this.toUserProfile(userInfo)),
      tap(user => this.saveUser(user))
    );
  }

  updateProfile(body: Partial<UserProfile>): Observable<UserProfile> {
    const existing = this.getUser();
    const payload: any = {
      username: body.username !== undefined ? body.username : existing?.username,
      email: body.email !== undefined ? body.email : existing?.email,
      fullName: body.fullName !== undefined ? body.fullName : existing?.fullName,
      age: body.age !== undefined ? body.age : existing?.age,
      phoneNumber: body.phoneNumber !== undefined ? body.phoneNumber : existing?.phoneNumber,
      address: body.address !== undefined ? body.address : existing?.address,
      gender: body.gender !== undefined ? body.gender : existing?.gender,
      workplace: body.workplace !== undefined ? body.workplace : existing?.workplace
    };
    return this.http.put<any>(`${API_BASE}/users/profile`, payload, { headers: this.authHeaders() }).pipe(
      map(userInfo => this.toUserProfile(userInfo)),
      tap(user => this.saveUser(user))
    );
  }

  private toUserProfile(userInfo: any): UserProfile {
    return {
      id: userInfo?.id ?? 0,
      username: userInfo?.username ?? '',
      fullName: userInfo?.fullName ?? userInfo?.username ?? '',
      age: userInfo?.age ?? 0,
      email: userInfo?.email ?? '',
      phoneNumber: userInfo?.phoneNumber ?? '',
      address: userInfo?.address ?? '',
      gender: userInfo?.gender ?? 'OTHER',
      workplace: userInfo?.workplace ?? '',
      role: userInfo?.role ?? 'BUYER',
      loyaltyPoints: userInfo?.loyaltyPoints ?? 0,
      loyaltyTier: userInfo?.loyaltyTier ?? 'BRONZE'
    };
  }

  // Seller Application Management
  submitSellerApplication(data: any): Observable<any> {
    return this.http.post<any>(`${API_BASE}/seller-applications`, data, { headers: this.authHeaders() });
  }

  getMySellerApplication(): Observable<any> {
    return this.http.get<any>(`${API_BASE}/seller-applications/my-application`, { headers: this.authHeaders() });
  }

  getAllSellerApplications(): Observable<any[]> {
    return this.http.get<any[]>(`${API_BASE}/seller-applications`, { headers: this.authHeaders() });
  }

  reviewSellerApplication(id: number, approved: boolean, reason?: string): Observable<any> {
    return this.http.post<any>(`${API_BASE}/seller-applications/${id}/review`, { approved, rejectionReason: reason }, { headers: this.authHeaders() });
  }
}
