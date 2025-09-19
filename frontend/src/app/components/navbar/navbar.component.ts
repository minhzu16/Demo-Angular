import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavLeftComponent } from './nav-left/nav-left.component';
import { NavRightComponent } from './nav-right/nav-right.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, NavLeftComponent, NavRightComponent],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent {
  @Input() collapsed: boolean = false;
  @Output() toggleSidebar = new EventEmitter<void>();
}
