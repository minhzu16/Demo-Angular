import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter, withViewTransitions } from '@angular/router';
import { HttpInterceptorFn, provideHttpClient, withInterceptors, HttpHeaders, withFetch } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ToastrModule } from 'ngx-toastr';
import { NgChartsModule } from 'ng2-charts';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

import { routes } from './app.routes';
import { errorInterceptor } from './core/interceptors/error.interceptor';

const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('access_token');
  // Skip adding token for login and register endpoints to avoid stale token issues
  const isAuthEndpoint = req.url.includes('/auth/login') || req.url.includes('/auth/register');

  if (token && !isAuthEndpoint) {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
    req = req.clone({ headers: req.headers.set('Authorization', `Bearer ${token}`) });
  }
  return next(req);
};

const userHeaderInterceptor: HttpInterceptorFn = (req, next) => {
  const userStr = localStorage.getItem('user_profile');
  const isAuthEndpoint = req.url.includes('/auth/login') || req.url.includes('/auth/register');

  if (userStr && !isAuthEndpoint) {
    try {
      const user = JSON.parse(userStr);
      let headers = req.headers;

      if (user && user.id != null) {
        headers = headers.set('X-User-Id', String(user.id));
      }
      if (user && user.username) {
        headers = headers.set('X-Username', user.username);
      }

      req = req.clone({ headers });
    } catch (e) { }
  }
  return next(req);
};

import { HttpClient } from '@angular/common/http';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { TranslateModule, TranslateLoader } from '@ngx-translate/core';

export function HttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http, './assets/i18n/', '.json');
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withViewTransitions()),
    provideHttpClient(
      withFetch(),
      withInterceptors([jwtInterceptor, userHeaderInterceptor, errorInterceptor])
    ),

    importProvidersFrom(
      BrowserAnimationsModule,
      TranslateModule.forRoot({
        defaultLanguage: 'en',
        loader: {
          provide: TranslateLoader,
          useFactory: HttpLoaderFactory,
          deps: [HttpClient]
        }
      }),
      // ✅ BUG 39 FIX: Prevent toast stacking — limit max toasts and auto-dismiss oldest
      ToastrModule.forRoot({
        timeOut: 3000,
        positionClass: 'toast-bottom-right',
        preventDuplicates: true,
        maxOpened: 3,
        autoDismiss: true,
        closeButton: true,
        progressBar: true
      }),
      NgChartsModule
    )
  ]
};
