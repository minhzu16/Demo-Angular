import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { AdminLayoutComponent } from './layouts/admin-layout/admin-layout.component';
import { GuestLayoutComponent } from './layouts/guest-layout/guest-layout.component';
import { authGuard } from './guards/auth.guard';
import { ProductListComponent } from './pages/product-list/product-list.component';
import { ProductDetailComponent } from './pages/product-detail/product-detail.component';
import { AdminProductFormComponent } from './pages/admin-product-form/admin-product-form.component';
import { AdminProductListComponent } from './pages/admin-product-list/admin-product-list.component';
import { AdminOrderListComponent } from './pages/admin-order-list/admin-order-list.component';
import { AdminInventoryComponent } from './pages/admin-inventory/admin-inventory.component';

export const routes: Routes = [
  // Guest routes (no layout)
  {
    path: '',
    component: GuestLayoutComponent,
    children: [
      { path: 'login', component: LoginComponent },
      { path: 'register', component: RegisterComponent },
      { path: 'products', component: ProductListComponent },
      { path: 'products/:id', component: ProductDetailComponent },
      { path: '', pathMatch: 'full', redirectTo: 'products' }
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
      { path: 'products', component: AdminProductListComponent },
      { path: 'products/new', component: AdminProductFormComponent },
      { path: 'products/:id', component: AdminProductFormComponent },
      { path: 'orders', component: AdminOrderListComponent },
      { path: 'orders/:id', component: AdminOrderListComponent },
      { path: 'inventory', component: AdminInventoryComponent },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },
  
  // Legacy routes for backward compatibility
  { path: 'dashboard', redirectTo: 'admin/dashboard' },
  { path: 'profile', redirectTo: 'admin/profile' },
  
  // Catch all
  { path: '**', redirectTo: 'products' }
];
