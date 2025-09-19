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
              console.log('Token saved to localStorage');
              if (response.user) {
                this.saveUser(response.user);
                console.log('User profile saved to localStorage');
              } else {
                console.log('No user profile in response');
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
      console.log('Token saved to localStorage:', token.substring(0, 20) + '...');
    }

    getToken(): string | null {
      const token = localStorage.getItem(TOKEN_KEY);
      console.log('Getting token from localStorage:', token ? token.substring(0, 20) + '...' : 'null');
      return token;
    }

    saveUser(user: UserProfile) {
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    }

    getUser(): UserProfile | null {
      const userStr = localStorage.getItem(USER_KEY);
      return userStr ? JSON.parse(userStr) : null;
    }

    isAuthenticated(): boolean {
      const token = this.getToken();
      if (!token) {
        console.log('No token found');
        return false;
      }
      
      // Check if token is expired (basic check)
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const currentTime = Date.now() / 1000;
        if (payload.exp && payload.exp < currentTime) {
          console.log('Token expired, removing from storage');
          // Don't call logout() here to avoid redirect loop
          localStorage.removeItem(TOKEN_KEY);
          localStorage.removeItem(USER_KEY);
          return false;
        }
        console.log('Token is valid');
        return true;
      } catch (error) {
        console.error('Error parsing token:', error);
        // Don't call logout() here to avoid redirect loop
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
      return this.http.get<UserProfile>(`${API_BASE}/api/auth/profile`, { headers: this.authHeaders() });
    }

    updateProfile(body: Partial<UserProfile>): Observable<UserProfile> {
      return this.http.put<UserProfile>(`${API_BASE}/api/auth/profile`, body, { headers: this.authHeaders() });
    }
  }
