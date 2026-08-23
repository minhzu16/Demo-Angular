import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterModule],
  template: `
    <div class="not-found-page">
      <div class="nf-content">
        <div class="nf-code">404</div>
        <h1 class="nf-title">Trang không tồn tại</h1>
        <p class="nf-desc">Trang bạn đang tìm kiếm có thể đã bị xóa, đổi tên hoặc tạm thời không khả dụng.</p>
        <div class="nf-actions">
          <a routerLink="/home" class="btn-home">
            <i class="bi bi-house-fill me-2"></i>Về trang chủ
          </a>
          <a routerLink="/products" class="btn-shop">
            <i class="bi bi-grid me-2"></i>Xem sản phẩm
          </a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .not-found-page {
      min-height: 70vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f8fafc;
      padding: 2rem;
    }
    .nf-content { text-align: center; max-width: 480px; }
    .nf-code {
      font-size: 8rem;
      font-weight: 800;
      color: #e2e8f0;
      line-height: 1;
      margin-bottom: 1rem;
      background: linear-gradient(135deg, #1a73e8, #0d47a1);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
    .nf-title { font-size: 1.75rem; font-weight: 700; color: #1e293b; margin-bottom: 0.75rem; }
    .nf-desc { color: #64748b; line-height: 1.6; margin-bottom: 2rem; }
    .nf-actions { display: flex; gap: 1rem; justify-content: center; flex-wrap: wrap; }
    .btn-home, .btn-shop {
      display: inline-flex; align-items: center;
      padding: 0.75rem 1.5rem;
      border-radius: 10px;
      font-weight: 600;
      text-decoration: none;
      transition: opacity 0.2s;
    }
    .btn-home { background: linear-gradient(135deg, #1a73e8, #0d47a1); color: white; }
    .btn-shop { background: #f1f5f9; color: #1e293b; }
    .btn-home:hover, .btn-shop:hover { opacity: 0.88; color: inherit; }
  `]
})
export class NotFoundComponent {}
