import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from '../../shared/header/header.component';
import { FooterComponent } from '../../shared/footer/footer.component';

@Component({
  selector: 'app-guest-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, HeaderComponent, FooterComponent],
  template: `
    <div class="storefront-layout">
      <app-header></app-header>
      <main class="storefront-content" id="main-content" tabindex="-1">
        <router-outlet></router-outlet>
      </main>
      <app-footer></app-footer>
    </div>
  `,
  styles: [`
    .storefront-layout { display: flex; flex-direction: column; min-height: 100vh; }
    /* The header is sticky (in flow), so no fixed top padding is needed. */
    .storefront-content { flex: 1; padding-top: 0; outline: none; }
  `]
})
export class GuestLayoutComponent {}

