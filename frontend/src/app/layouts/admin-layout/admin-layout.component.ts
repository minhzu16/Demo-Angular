import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { NavigationComponent } from '../../components/navigation/navigation.component';
import { BreadcrumbComponent, BreadcrumbItem } from '../../components/breadcrumb/breadcrumb.component';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent, NavigationComponent, BreadcrumbComponent],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss'
})
export class AdminLayoutComponent {
  sidebarCollapsed = false;
  breadcrumbItems = [
    { label: 'Home', url: '/admin/dashboard' },
    { label: 'Dashboard', active: true }
  ];

  onToggleSidebar() {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }
}
