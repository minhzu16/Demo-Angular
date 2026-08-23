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
      url: '/dashboard',
      active: true
    },
    {
      label: 'Profile',
      icon: 'user',
      url: '/profile'
    },
    {
      label: 'Orders',
      icon: 'shopping-cart',
      url: '/orders',
      children: [
        { label: 'All Orders', url: '/orders' },
        { label: 'Pending', url: '/orders/pending' },
        { label: 'Completed', url: '/orders/completed' }
      ]
    },
    {
      label: 'Products',
      icon: 'inbox',
      url: '/products',
      children: [
        { label: 'All Products', url: '/products' },
        { label: 'Categories', url: '/products/categories' },
        { label: 'Inventory', url: '/products/inventory' }
      ]
    },
    {
      label: 'Customers',
      icon: 'team',
      url: '/customers'
    },
    {
      label: 'Analytics',
      icon: 'bar-chart',
      url: '/analytics'
    },
    {
      label: 'Settings',
      icon: 'setting',
      url: '/settings'
    }
  ];

  onToggleCollapse() {
    this.toggleCollapse.emit();
  }

  navigateTo(url: string) {
    this.router.navigate([url]).catch(err => {
      console.error('Navigation error:', err);
    });
  }
}
