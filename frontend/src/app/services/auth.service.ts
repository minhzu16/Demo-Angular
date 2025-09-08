  import { Injectable, inject } from '@angular/core';
  import { HttpClient, HttpErrorResponse } from '@angular/common/http';
  import { Router } from '@angular/router';
  import { catchError, tap } from 'rxjs/operators';
  import { Observable, throwError } from 'rxjs';

  interface LoginRequest { username: string; password: string; }
  interface LoginResponse { accessToken: string; }

  const TOKEN_KEY = 'access_token';
  const API_BASE = 'http://localhost:8080';

  @Injectable({ providedIn: 'root' })
  export class AuthService {
    private http = inject(HttpClient);
    private router = inject(Router);

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

    isAuthenticated(): boolean {
      return !!this.getToken();
    }

    logout() {
      localStorage.removeItem(TOKEN_KEY);
      this.router.navigateByUrl('/login');
    }
  }
