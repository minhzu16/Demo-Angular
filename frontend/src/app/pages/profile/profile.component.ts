import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { AuthService, UserProfile } from '../../services/auth.service';
import { ShopService } from '../../services/shop.service';
import { SellerApplicationService, SellerApplication } from '../../services/seller-application.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  user: UserProfile | null = null;
  private original: UserProfile | null = null;
  isEditing = false;
  error = '';
  success = '';
  sellerStatus: 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED' = 'NONE';
  isLoadingSeller = false;
  hasShop = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private shopService: ShopService,
    private sellerAppService: SellerApplicationService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.loadUserProfile();
    this.loadSellerState();

    const path = this.route.snapshot.routeConfig?.path;
    if (path === 'profile/edit') {
      this.isEditing = true;
    }
  }

  loadUserProfile() {
    console.log('Loading user profile...');
    this.user = this.authService.getUser();
    console.log('User from localStorage:', this.user);
    
    if (!this.user) {
      console.log('No user in localStorage, fetching from API...');
      this.authService.getProfile().subscribe({
        next: (profile: UserProfile) => {
          console.log('Profile fetched from API:', profile);
          this.user = profile;
          this.authService.saveUser(profile);
          this.original = JSON.parse(JSON.stringify(profile));
        },
        error: (err: any) => {
          console.error('Error loading profile:', err);
          this.error = 'Không thể tải thông tin cá nhân';
          // If profile fetch fails, redirect to login
          if (err.status === 401 || err.status === 403) {
            console.log('Unauthorized, redirecting to login...');
            this.authService.logout();
          }
        }
      });
    }
    if (this.user && !this.original) {
      this.original = JSON.parse(JSON.stringify(this.user));
    }
  }

  toggleEdit() {
    this.isEditing = !this.isEditing;
    this.error = '';
    this.success = '';
    if (this.isEditing && this.user) {
      // chụp lại bản gốc khi bắt đầu edit
      this.original = JSON.parse(JSON.stringify(this.user));
    }
  }

  saveProfile() {
    if (!this.user) return;
    if (this.isUnchanged()) {
      this.success = 'Không có thay đổi để lưu';
      this.isEditing = false;
      return;
    }

    this.authService.updateProfile(this.user).subscribe({
      next: (updatedProfile: UserProfile) => {
        this.user = updatedProfile;
        this.authService.saveUser(updatedProfile);
        this.success = 'Cập nhật thông tin thành công!';
        this.error = '';
        this.original = JSON.parse(JSON.stringify(updatedProfile));

        const path = this.route.snapshot.routeConfig?.path;
        if (path === 'profile/edit') {
          this.router.navigate(['/profile']);
        } else {
          this.isEditing = false;
        }
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Cập nhật thất bại';
        console.error('Error updating profile:', err);
      }
    });
  }

  cancelEdit() {
    const path = this.route.snapshot.routeConfig?.path;
    if (path === 'profile/edit') {
      this.router.navigate(['/profile']);
      return;
    }

    this.loadUserProfile();
    this.isEditing = false;
    this.error = '';
    this.success = '';
  }

  getGenderText(gender?: string | null): string {
    switch (gender) {
      case 'MALE': return 'Nam';
      case 'FEMALE': return 'Nữ';
      case 'OTHER': return 'Khác';
      default: return 'Chưa cập nhật';
    }
  }

  goBack() {
    const currentUrl = this.router.url;
    if (currentUrl.startsWith('/admin')) {
      this.router.navigate(['/admin/dashboard']);
    } else {
      this.router.navigate(['/products']);
    }
  }

  switchToSeller() {
    if (this.canGoToShop()) {
      this.router.navigate(['/seller/dashboard']);
    } else {
      alert('Bạn chưa được duyệt bán hàng. Vui lòng đăng ký hoặc chờ admin duyệt.');
    }
  }

  isUnchanged(): boolean {
    if (!this.user || !this.original) return false;
    const a = this.user;
    const b = this.original;
    return (
      a.fullName === b.fullName &&
      a.age === b.age &&
      a.email === b.email &&
      (a.phoneNumber || '') === (b.phoneNumber || '') &&
      (a.address || '') === (b.address || '') &&
      (a.gender || '') === (b.gender || '') &&
      (a.workplace || '') === (b.workplace || '')
    );
  }

  loadSellerState() {
    this.isLoadingSeller = true;
    this.hasShop = false;
    this.sellerStatus = 'NONE';

    this.shopService.getMyShop().subscribe({
      next: () => {
        this.hasShop = true;
        this.sellerStatus = 'APPROVED';
        this.isLoadingSeller = false;
      },
      error: () => {
        this.loadSellerApplication();
      }
    });
  }

  private loadSellerApplication() {
    this.sellerAppService.getMyApplication().subscribe({
      next: (app: SellerApplication | null) => {
        if (app && app.status) {
          const status = app.status as 'PENDING' | 'APPROVED' | 'REJECTED';
          this.sellerStatus = status;
        } else {
          this.sellerStatus = 'NONE';
        }
        this.isLoadingSeller = false;
      },
      error: (err: any) => {
        if (err.status === 404) {
          this.sellerStatus = 'NONE';
        }
        this.isLoadingSeller = false;
      }
    });
  }

  getSellerStatusText(status: 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED'): string {
    switch (status) {
      case 'PENDING':
        return 'Đơn đăng ký bán hàng của bạn đang chờ duyệt.';
      case 'APPROVED':
        return 'Đơn đăng ký bán hàng đã được duyệt. Bạn có thể truy cập Cửa hàng cá nhân.';
      case 'REJECTED':
        return 'Đơn đăng ký bán hàng bị từ chối. Vui lòng kiểm tra lại thông tin hoặc liên hệ hỗ trợ.';
      default:
        return '';
    }
  }

  canGoToShop(): boolean {
    return this.hasShop || this.sellerStatus === 'APPROVED';
  }

  canApplySeller(): boolean {
    return !this.canGoToShop() && (this.sellerStatus === 'NONE' || this.sellerStatus === 'REJECTED');
  }

  applySeller() {
    if (!this.user) return;

    const body = {
      shopName: this.user.fullName || this.user.username,
      shopDescription: '',
      businessLicense: '',
      taxCode: '',
      phoneNumber: this.user.phoneNumber || '',
      address: this.user.address || '',
      categoryIds: [1]
    };

    this.isLoadingSeller = true;
    this.sellerAppService.submitApplication(body).subscribe({
      next: (app: SellerApplication) => {
        if (app && app.status) {
          const status = app.status as 'PENDING' | 'APPROVED' | 'REJECTED';
          this.sellerStatus = status;
        }
        this.isLoadingSeller = false;
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Đăng ký bán hàng thất bại';
        this.isLoadingSeller = false;
      }
    });
  }
}
