import { Component, Input, OnInit, ViewChild, ElementRef, AfterViewInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-pie-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pie-chart.component.html',
  styleUrl: './pie-chart.component.scss'
})
export class PieChartComponent implements OnInit, AfterViewInit, OnChanges {
  @ViewChild('pieChartCanvas') pieChartCanvas!: ElementRef;
  @Input() data: number[] = [];
  @Input() labels: string[] = [];
  @Input() title: string = 'Order Status Distribution';

  private chart: any = null;

  ngOnInit() {}

  ngAfterViewInit() {
    this.createChart();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.chart && (changes['data'] || changes['labels'])) {
      this.updateChart();
    }
  }

  createChart() {
    const ctx = this.pieChartCanvas.nativeElement.getContext('2d');
    
    this.chart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: this.labels,
        datasets: [{
          data: this.data,
          backgroundColor: [
            '#0d6efd', // Primary
            '#198754', // Success
            '#ffc107', // Warning
            '#dc3545', // Danger
            '#0dcaf0'  // Info
          ],
          borderWidth: 0,
          hoverOffset: 12
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '70%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              usePointStyle: true,
              padding: 20,
              font: { size: 12 }
            }
          },
          tooltip: {
            backgroundColor: '#1e293b',
            padding: 12,
            displayColors: true
          }
        }
      }
    });
  }

  updateChart() {
    if (this.chart) {
      this.chart.data.labels = this.labels;
      this.chart.data.datasets[0].data = this.data;
      this.chart.update();
    }
  }
}
