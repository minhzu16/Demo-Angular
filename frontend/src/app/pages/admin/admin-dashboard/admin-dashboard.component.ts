import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnalyticsService, SalesOverview } from '../../../services/analytics.service';
import { RouterModule } from '@angular/router';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, NgChartsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  analyticsService = inject(AnalyticsService);
  overview: SalesOverview | null = null;
  loading = true;

  // Chart Properties
  public lineChartData: ChartData<'line'> = {
    datasets: [],
    labels: []
  };

  public lineChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: true },
    }
  };

  ngOnInit() {
    this.loadStats();
    this.loadChartData();
  }

  loadStats() {
    const end = new Date().toISOString().split('T')[0];
    const start = new Date(new Date().setDate(new Date().getDate() - 30)).toISOString().split('T')[0];
    
    this.analyticsService.getSalesOverview(start, end).subscribe({
      next: (data) => {
        this.overview = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadChartData() {
    const end = new Date().toISOString().split('T')[0];
    const start = new Date(new Date().setDate(new Date().getDate() - 7)).toISOString().split('T')[0];
    
    // For demo using shopId 1
    this.analyticsService.getRevenue(1, start, end, 'daily').subscribe({
      next: (data) => {
        this.lineChartData = {
          labels: data.map(d => d.date),
          datasets: [
            {
              data: data.map(d => d.revenue),
              label: 'Doanh thu (VND)',
              backgroundColor: 'rgba(13, 110, 253, 0.2)',
              borderColor: 'rgba(13, 110, 253, 1)',
              fill: 'origin',
            }
          ]
        };
      }
    });
  }
}
