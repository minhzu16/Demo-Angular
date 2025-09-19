import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-bar-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bar-chart.component.html',
  styleUrl: './bar-chart.component.scss'
})
export class BarChartComponent implements OnInit {
  @Input() data: any[] = [];
  @Input() labels: string[] = [];
  @Input() title: string = 'Chart';

  ngOnInit() {
    // Chart.js integration would go here
    // For now, we'll use a placeholder
  }
}
