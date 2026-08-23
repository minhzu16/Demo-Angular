import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { GuestLayoutComponent } from './layouts/guest-layout/guest-layout.component';

export const routes: Routes = [
  // ── Storefront (buyer-facing) ──────────────────────────────
  {
    path: '',
    component: GuestLayoutComponent,
    children: [
      {
        path: 'home',
        loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent)
      },
      {
        path: 'products',
        loadComponent: () => import('./pages/products/products.component').then(m => m.ProductsComponent)
      },
      {
        path: 'products/:id',
        loadComponent: () => import('./pages/product-detail/product-detail.component').then(m => m.ProductDetailComponent)
      },
      {
        path: 'compare',
        loadComponent: () => import('./pages/compare/compare.component').then(m => m.CompareComponent)
      },
      {
        path: 'cart',
        loadComponent: () => import('./pages/cart/cart.component').then(m => m.CartComponent),
        canActivate: [authGuard]
      },
      {
        path: 'checkout',
        loadComponent: () => import('./pages/checkout/checkout.component').then(m => m.CheckoutComponent),
        canActivate: [authGuard]
      },
      {
        path: 'orders',
        loadComponent: () => import('./pages/orders/orders.component').then(m => m.OrdersComponent),
        canActivate: [authGuard]
      },
      {
        path: 'orders/:id',
        loadComponent: () => import('./pages/orders/order-detail.component').then(m => m.OrderDetailComponent),
        canActivate: [authGuard]
      },
      {
        path: 'profile',
        loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent),
        canActivate: [authGuard]
      },
      {
        path: 'wishlist',
        loadComponent: () => import('./pages/wishlist/wishlist.component').then(m => m.WishlistComponent),
        canActivate: [authGuard]
      },
      {
        path: 'orders/:id/success',
        loadComponent: () => import('./pages/order-success/order-success.component').then(m => m.OrderSuccessComponent),
        canActivate: [authGuard]
      },
      { path: '', redirectTo: 'home', pathMatch: 'full' }
    ]
  },

  // ── Auth ──────────────────────────────────────────────────
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent)
  },

  // ── Admin ─────────────────────────────────────────────────
  {
    path: 'dashboard',
    loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'admin',
    loadComponent: () => import('./pages/admin/admin-layout/admin-layout.component').then(m => m.AdminLayoutComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/admin/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent)
      },
      {
        path: 'products',
        loadComponent: () => import('./pages/admin/admin-products/admin-products.component').then(m => m.AdminProductsComponent)
      },
      {
        path: 'flash-sales',
        loadComponent: () => import('./pages/admin/flash-sales/admin-flash-sales.component').then(m => m.AdminFlashSalesComponent)
      },
      {
        path: 'sellers',
        loadComponent: () => import('./pages/admin/sellers/admin-sellers.component').then(m => m.AdminSellersComponent)
      },
      {
        path: 'users',
        loadComponent: () => import('./pages/admin/admin-users/admin-users.component').then(m => m.AdminUsersComponent)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  // ── Seller ────────────────────────────────────────────────
  {
    path: 'seller/dashboard',
    loadComponent: () => import('./pages/seller/dashboard/seller-dashboard.component').then(m => m.SellerDashboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'seller/products',
    loadComponent: () => import('./pages/seller/products/seller-products.component').then(m => m.SellerProductsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'seller/products/new',
    loadComponent: () => import('./pages/seller/products/seller-product-form.component').then(m => m.SellerProductFormComponent),
    canActivate: [authGuard]
  },
  {
    path: 'seller/products/edit/:id',
    loadComponent: () => import('./pages/seller/products/seller-product-form.component').then(m => m.SellerProductFormComponent),
    canActivate: [authGuard]
  },
  {
    path: 'seller/orders',
    loadComponent: () => import('./pages/seller/orders/seller-orders.component').then(m => m.SellerOrdersComponent),
    canActivate: [authGuard]
  },
  {
    path: 'seller/chat',
    loadComponent: () => import('./pages/seller/chat/seller-chat.component').then(m => m.SellerChatComponent),
    canActivate: [authGuard]
  },

  // ── 404 ──────────────────────────────────────────────────
  {
    path: '**',
    loadComponent: () => import('./pages/not-found/not-found.component').then(m => m.NotFoundComponent)
  }
];
