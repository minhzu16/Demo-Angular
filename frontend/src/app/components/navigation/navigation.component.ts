import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navigation',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navigation.component.html',
  styleUrl: './navigation.component.scss'
})
export class NavigationComponent {
  @Input() collapsed: boolean = false;
  @Output() toggleCollapse = new EventEmitter<void>();
  
  private router = inject(Router);
  private authService = inject(AuthService);

  menuItems = [
    {
      label: 'Dashboard',
      icon: 'dashboard',
      url: '/admin/dashboard',
      active: true
    },
    {
      label: 'Profile',
      icon: 'user',
      url: '/admin/profile'
    },
    {
      label: 'Orders',
      icon: 'shopping-cart',
      url: '/admin/orders',
      children: [
        { label: 'All Orders', url: '/admin/orders' },
        { label: 'Pending', url: '/admin/orders/pending' },
        { label: 'Completed', url: '/admin/orders/completed' }
      ]
    },
    {
      label: 'Products',
      icon: 'inbox',
      url: '/admin/products',
      children: [
        { label: 'All Products', url: '/admin/products' },
        { label: 'Categories', url: '/admin/products/categories' },
        { label: 'Inventory', url: '/admin/products/inventory' }
      ]
    },
    {
      label: 'Customers',
      icon: 'team',
      url: '/admin/customers'
    },
    {
      label: 'Analytics',
      icon: 'bar-chart',
      url: '/admin/analytics'
    },
    {
      label: 'Settings',
      icon: 'setting',
      url: '/admin/settings'
    }
  ];

  onToggleCollapse() {
    this.toggleCollapse.emit();
  }

  navigateTo(url: string) {
    console.log('Navigation component navigating to:', url);
    console.log('Current auth status:', this.authService.isAuthenticated());
    console.log('Current token:', this.authService.getToken() ? 'exists' : 'null');
    
    this.router.navigate([url]).then(success => {
      console.log('Navigation result:', success);
    }).catch(err => {
      console.error('Navigation error:', err);
    });
  }
}
