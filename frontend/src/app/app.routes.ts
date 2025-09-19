import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { AdminLayoutComponent } from './layouts/admin-layout/admin-layout.component';
import { GuestLayoutComponent } from './layouts/guest-layout/guest-layout.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  // Guest routes (no layout)
  {
    path: '',
    component: GuestLayoutComponent,
    children: [
      { path: 'login', component: LoginComponent },
      { path: 'register', component: RegisterComponent },
      { path: '', pathMatch: 'full', redirectTo: 'login' }
    ]
  },
  
  // Admin routes (with admin layout)
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'profile', component: ProfileComponent },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },
  
  // Legacy routes for backward compatibility
  { path: 'dashboard', redirectTo: 'admin/dashboard' },
  { path: 'profile', redirectTo: 'admin/profile' },
  
  // Catch all
  { path: '**', redirectTo: 'login' }
];
