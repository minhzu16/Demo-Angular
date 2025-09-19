import { Component, inject, OnInit, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { CardComponent } from '../../components/card/card.component';
import { BarChartComponent } from '../../components/charts/bar-chart/bar-chart.component';
import { PieChartComponent } from '../../components/charts/pie-chart/pie-chart.component';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, CardComponent, BarChartComponent, PieChartComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, AfterViewInit {
  private router = inject(Router);
  private authService = inject(AuthService);

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    // Ensure dashboard view is rendered before showing alert
    setTimeout(() => {
      const state: any = history.state || {};
      const fromLogin = state.fromLogin === true;
      const nameFromState = state.fullName as string | undefined;
      console.log('Dashboard after view init - nav state:', state);

      const token = this.authService.getToken();
      const welcomeKey = `welcome_shown_${token ? token.slice(0, 10) : 'guest'}`;
      const hasShown = sessionStorage.getItem(welcomeKey);

      const show = (name?: string) => {
        if (hasShown) return;
        if (name) alert(`Chào mừng, ${name}!`);
        sessionStorage.setItem(welcomeKey, 'true');
      };

      if (fromLogin) {
        if (nameFromState) {
          show(nameFromState);
          return;
        }
        const user = this.authService.getUser();
        if (user) {
          show(user.fullName || user.username);
          return;
        }
      }

      const existingUser = this.authService.getUser();
      if (existingUser) {
        show(existingUser.fullName || existingUser.username);
        return;
      }
      this.authService.getProfile().subscribe({
        next: profile => {
          this.authService.saveUser(profile);
          show(profile.fullName || profile.username);
        },
        error: () => sessionStorage.setItem(welcomeKey, 'true')
      });
    }, 0);
  }
  

  goToProfile() {
    console.log('Navigating to profile...');
    console.log('Current auth status:', this.authService.isAuthenticated());
    console.log('Current token:', this.authService.getToken() ? 'exists' : 'null');
    
    this.router.navigate(['/admin/profile']).then(success => {
      console.log('Profile navigation result:', success);
    }).catch(err => {
      console.error('Profile navigation error:', err);
    });
  }

  stats = [
    {
      title: 'Total Revenue',
      value: '$45,231',
      description: 'vs last month',
      icon: 'dollar-circle',
      badge: '+12.5%',
      type: 'stat' as const,
      badgeType: 'success' as const
    },
    {
      title: 'Total Orders',
      value: '2,350',
      description: 'vs last month',
      icon: 'shopping-cart',
      badge: '+8.2%',
      type: 'stat' as const,
      badgeType: 'info' as const
    },
    {
      title: 'Total Customers',
      value: '1,234',
      description: 'vs last month',
      icon: 'user',
      badge: '+15.3%',
      type: 'stat' as const,
      badgeType: 'warning' as const
    },
    {
      title: 'Conversion Rate',
      value: '3.24%',
      description: 'vs last month',
      icon: 'line-chart',
      badge: '-2.1%',
      type: 'stat' as const,
      badgeType: 'danger' as const
    }
  ];

  recentOrders = [
    { id: '#1234', customer: 'John Doe', product: 'Laptop', amount: '$1,299', status: 'completed' },
    { id: '#1235', customer: 'Jane Smith', product: 'Phone', amount: '$699', status: 'pending' },
    { id: '#1236', customer: 'Bob Johnson', product: 'Tablet', amount: '$499', status: 'shipped' },
    { id: '#1237', customer: 'Alice Brown', product: 'Headphones', amount: '$199', status: 'completed' },
    { id: '#1238', customer: 'Charlie Wilson', product: 'Monitor', amount: '$399', status: 'pending' }
  ];

  transactions = [
    { id: 1, title: 'Payment from John Doe', time: '2 min ago', amount: '+$1,299', percentage: '+12.5%', avatar: 'JD' },
    { id: 2, title: 'Payment from Jane Smith', time: '5 min ago', amount: '+$699', percentage: '+8.2%', avatar: 'JS' },
    { id: 3, title: 'Payment from Bob Johnson', time: '1 hour ago', amount: '+$499', percentage: '+15.3%', avatar: 'BJ' },
    { id: 4, title: 'Payment from Alice Brown', time: '2 hours ago', amount: '+$199', percentage: '+5.1%', avatar: 'AB' },
    { id: 5, title: 'Payment from Charlie Wilson', time: '3 hours ago', amount: '+$399', percentage: '+9.8%', avatar: 'CW' }
  ];

  supportTeam = [
    { name: 'Sarah', avatar: 'S' },
    { name: 'Mike', avatar: 'M' },
    { name: 'Lisa', avatar: 'L' },
    { name: 'Tom', avatar: 'T' }
  ];

  getStatusClass(status: string): string {
    switch (status) {
      case 'completed': return 'success';
      case 'pending': return 'warning';
      case 'shipped': return 'info';
      default: return 'secondary';
    }
  }

  getAvatarColor(index: number): string {
    const colors = ['bg-primary', 'bg-success', 'bg-warning', 'bg-danger', 'bg-info'];
    return colors[index % colors.length];
  }
}
