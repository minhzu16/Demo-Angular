import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './card.component.html',
  styleUrl: './card.component.scss'
})
export class CardComponent {
  @Input() title: string = '';
  @Input() value: string | number = '';
  @Input() description: string = '';
  @Input() icon: string = '';
  @Input() badge: string = '';
  @Input() badgeType: 'success' | 'warning' | 'danger' | 'info' = 'info';
  @Input() type: 'stat' | 'default' = 'default';
  @Input() class: string = '';
}