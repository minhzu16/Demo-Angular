import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HttpInterceptorFn, provideHttpClient, withInterceptors } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ToastrModule } from 'ngx-toastr';

import { routes } from './app.routes';
import { errorInterceptor } from './core/interceptors/error.interceptor';
  
const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};

const userHeaderInterceptor: HttpInterceptorFn = (req, next) => {
  const userStr = localStorage.getItem('user_profile');
  if (userStr) {
    try {
      const user = JSON.parse(userStr);
      const headers: { [key: string]: string } = {};
      if (user && user.id != null) {
        headers['X-User-Id'] = String(user.id);
      }
      if (user && user.username) {
        headers['X-Username'] = user.username;
      }
      if (Object.keys(headers).length > 0) {
        const currentHeaders: { [key: string]: string } = {};
        req.headers.keys().forEach(name => {
          const value = req.headers.get(name);
          if (value !== null) {
            currentHeaders[name] = value;
          }
        });
        req = req.clone({ setHeaders: { ...currentHeaders, ...headers } });
      }
    } catch (e) {}
  }
  return next(req);
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor, userHeaderInterceptor, errorInterceptor])),
    importProvidersFrom(
      BrowserAnimationsModule,
      ToastrModule.forRoot({
        timeOut: 3000,
        positionClass: 'toast-bottom-right',
        preventDuplicates: true
      })
    )
  ]
};
