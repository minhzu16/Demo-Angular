import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-admin-products',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-products.component.html'
})
export class AdminProductsComponent implements OnInit {
  private http = inject(HttpClient);
  private toastr = inject(ToastrService);

  products: any[] = [];
  page = 1;
  size = 10;
  totalElements = 0;
  loading = false;
  currentTab = 'PENDING';

  ngOnInit() {
    this.loadProducts();
  }

  setTab(tab: string) {
    this.currentTab = tab;
    this.page = 1;
    this.loadProducts();
  }

  loadProducts() {
    this.loading = true;
    // Tạm thời gọi API lấy list bình thường, vì filter bằng status chưa hoàn thiện trên BE search. 
    // Nhưng ta sẽ hiển thị tất cả và cho phép Admin đổi status
    this.http.get<any>(`${environment.apiBaseUrl}/admin/products?page=${this.page - 1}&size=${this.size}`).subscribe({
      next: (res) => {
        // Lọc tay theo tab nếu BE trả về hết (thực tế nên dùng BE query param)
        const allProducts = res.content || [];
        if (this.currentTab === 'ALL') {
             this.products = allProducts;
        } else {
             this.products = allProducts.filter((p: any) => p.status === this.currentTab);
        }
        // Vì filter tay ở FE, phân trang sẽ không hoàn hảo.
        // Tốt nhất là hiện tất cả và phân biệt màu status
        this.products = allProducts;
        this.totalElements = res.totalElements || 0;
        this.loading = false;
      },
      error: () => {
        this.toastr.error('Lỗi khi tải danh sách sản phẩm');
        this.loading = false;
      }
    });
  }

  onPageChange(newPage: number) {
    this.page = newPage;
    this.loadProducts();
  }

  updateStatus(product: any, status: string) {
    if (confirm(`Bạn muốn chuyển trạng thái sản phẩm sang ${status}?`)) {
      this.http.post<any>(`${environment.apiBaseUrl}/admin/products/${product.id}/status?status=${status}`, {}).subscribe({
        next: () => {
          this.toastr.success(`Đã cập nhật trạng thái`);
          product.status = status;
        },
        error: () => {
          this.toastr.error('Lỗi cập nhật trạng thái');
        }
      });
    }
  }

  getStatusBadgeClass(status: string) {
    switch (status) {
      case 'ACTIVE': return 'bg-success';
      case 'PENDING': return 'bg-warning text-dark';
      case 'REJECTED': return 'bg-danger';
      default: return 'bg-secondary';
    }
  }
}
