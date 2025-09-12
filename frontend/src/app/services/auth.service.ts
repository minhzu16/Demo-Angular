  import { Injectable, inject } from '@angular/core';
  import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
  import { Router } from '@angular/router';
  import { catchError, tap } from 'rxjs/operators';
  import { Observable, throwError } from 'rxjs';

  interface LoginRequest { username: string; password: string; }
  interface LoginResponse { accessToken: string; user: UserProfile; }
  export interface UserProfile { 
    id: number; 
    username: string; 
    fullName: string;
    age: number;
    email: string; 
    phoneNumber: string;
    address: string;
    gender: 'MALE' | 'FEMALE' | 'OTHER';
    workplace: string;
  }

  const TOKEN_KEY = 'access_token';
  const USER_KEY = 'user_profile';
  const API_BASE = 'http://localhost:8080';

  @Injectable({ providedIn: 'root' })
  export class AuthService {
    private http = inject(HttpClient);
    private router = inject(Router);
    private authHeaders(): HttpHeaders {
      const token = this.getToken();
      let headers = new HttpHeaders({ 'Content-Type': 'application/json' });
      if (token) {
        headers = headers.set('Authorization', `Bearer ${token}`);
      }
      return headers;
    }


    login(body: LoginRequest): Observable<LoginResponse> {
      console.log('Login Request:', {
        url: `${API_BASE}/api/auth/login`,
        body: body
      });
      
      return this.http.post<LoginResponse>(`${API_BASE}/api/auth/login`, body)
        .pipe(
          tap(response => {
            console.log('Login successful:', response); 
            if (response.accessToken) {
              this.saveToken(response.accessToken);
              if (response.user) {
                this.saveUser(response.user);
              }
            } else {
              console.error('No token in response');
            }
          }),
          catchError((error: HttpErrorResponse) => {
            console.error('Login failed:', {
              status: error.status,
              message: error.message,
              error: error.error
            });
            return throwError(() => error);
          })
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
      return !!this.getToken();
    }

    logout() {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      this.router.navigateByUrl('/login');
    }

    getProfile(): Observable<UserProfile> {
      return this.http.get<UserProfile>(`${API_BASE}/api/auth/profile`, { headers: this.authHeaders() });
    }

    updateProfile(body: Partial<UserProfile>): Observable<UserProfile> {
      return this.http.put<UserProfile>(`${API_BASE}/api/auth/profile`, body, { headers: this.authHeaders() });
    }
  }
