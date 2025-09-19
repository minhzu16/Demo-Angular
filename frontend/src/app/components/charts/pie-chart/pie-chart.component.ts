import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-pie-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pie-chart.component.html',
  styleUrl: './pie-chart.component.scss'
})
export class PieChartComponent implements OnInit {
  @Input() data: any[] = [];
  @Input() labels: string[] = [];
  @Input() title: string = 'Chart';

  ngOnInit() {
    // Chart.js integration would go here
    // For now, we'll use a placeholder
  }
}
