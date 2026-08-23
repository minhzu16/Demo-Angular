import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminService, AdminUser } from '../../../services/admin.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-users.component.html'
})
export class AdminUsersComponent implements OnInit {
  private adminService = inject(AdminService);
  private toastr = inject(ToastrService);

  users: AdminUser[] = [];
  page = 1;
  size = 10;
  totalElements = 0;
  loading = false;

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.loading = true;
    this.adminService.getUsers(this.page - 1, this.size).subscribe({
      next: (res) => {
        this.users = res.content || [];
        this.totalElements = res.totalElements || 0;
        this.loading = false;
      },
      error: () => {
        this.toastr.error('Lỗi khi tải danh sách người dùng');
        this.loading = false;
      }
    });
  }

  onPageChange(newPage: number) {
    this.page = newPage;
    this.loadUsers();
  }

  toggleBan(user: AdminUser) {
    const newStatus = user.status === 'ACTIVE' ? 'BANNED' : 'ACTIVE';
    if (confirm(`Bạn có chắc chắn muốn ${newStatus === 'BANNED' ? 'khóa' : 'mở khóa'} tài khoản ${user.username}?`)) {
      this.adminService.updateUserStatus(user.id, newStatus).subscribe({
        next: () => {
          this.toastr.success(`Đã cập nhật trạng thái tài khoản ${user.username}`);
          user.status = newStatus;
        },
        error: () => {
          this.toastr.error('Lỗi khi cập nhật trạng thái');
        }
      });
    }
  }

  getStatusBadgeClass(status: string) {
    return status === 'ACTIVE' ? 'bg-success' : 'bg-danger';
  }
}
