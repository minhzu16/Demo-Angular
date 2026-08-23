import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'app-admin-sellers',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './admin-sellers.component.html',
    styleUrls: ['./admin-sellers.component.scss']
})
export class AdminSellersComponent implements OnInit {
    private authService = inject(AuthService);
    private toastr = inject(ToastrService);

    applications: any[] = [];
    loading = false;
    showReviewModal = false;
    selectedApp: any = null;
    rejectionReason = '';

    ngOnInit() {
        this.loadApplications();
    }

    loadApplications() {
        this.loading = true;
        this.authService.getAllSellerApplications().subscribe({
            next: (res) => {
                this.applications = res || [];
                this.loading = false;
            },
            error: () => {
                this.toastr.error('Không thể tải danh sách đơn đăng ký');
                this.loading = false;
            }
        });
    }

    openReviewModal(app: any) {
        this.selectedApp = app;
        this.rejectionReason = '';
        this.showReviewModal = true;
    }

    closeModal() {
        this.showReviewModal = false;
        this.selectedApp = null;
    }

    approve() {
        if (!this.selectedApp) return;
        this.authService.reviewSellerApplication(this.selectedApp.id, true).subscribe({
            next: () => {
                this.toastr.success('Đã duyệt đơn đăng ký');
                this.loadApplications();
                this.closeModal();
            },
            error: () => this.toastr.error('Lỗi khi duyệt đơn')
        });
    }

    reject() {
        if (!this.selectedApp || !this.rejectionReason) {
            this.toastr.warning('Vui lòng nhập lý do từ chối');
            return;
        }
        this.authService.reviewSellerApplication(this.selectedApp.id, false, this.rejectionReason).subscribe({
            next: () => {
                this.toastr.success('Đã từ chối đơn đăng ký');
                this.loadApplications();
                this.closeModal();
            },
            error: () => this.toastr.error('Lỗi khi từ chối đơn')
        });
    }

    getStatusClass(status: string): string {
        switch (status) {
            case 'APPROVED': return 'badge bg-success';
            case 'PENDING': return 'badge bg-warning text-dark';
            case 'REJECTED': return 'badge bg-danger';
            default: return 'badge bg-secondary';
        }
    }
}
