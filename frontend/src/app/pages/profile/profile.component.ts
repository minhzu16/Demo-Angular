import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { AuthService, UserProfile, UserAddress, LoyaltyTransaction } from '../../services/auth.service';
import { ShopService } from '../../services/shop.service';
import { SellerApplicationService, SellerApplication } from '../../services/seller-application.service';
import { ToastrService } from 'ngx-toastr';

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
  addresses: UserAddress[] = [];
  isAddingAddress = false;
  newAddress: UserAddress = { receiverName: '', phoneNumber: '', address: '', isDefault: false };
  error = '';
  success = '';
  sellerStatus: 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED' = 'NONE';
  isLoadingSeller = false;
  hasShop = false;
  loyaltyHistory: LoyaltyTransaction[] = [];
  isLoadingLoyalty = false;


  constructor(
    private authService: AuthService,
    private router: Router,
    private shopService: ShopService,
    private sellerAppService: SellerApplicationService,
    private route: ActivatedRoute,
    private toastr: ToastrService
  ) { }

  ngOnInit() {
    this.loadUserProfile();
    this.loadSellerState();
    this.loadAddresses();
    this.loadLoyaltyHistory();

    const path = this.route.snapshot.routeConfig?.path;
    if (path === 'profile/edit') {
      this.isEditing = true;
    }
  }

  loadUserProfile() {
    this.user = this.authService.getUser();

    if (!this.user) {
      this.authService.getProfile().subscribe({
        next: (profile: UserProfile) => {
          this.user = profile;
          this.authService.saveUser(profile);
          this.original = JSON.parse(JSON.stringify(profile));
        },
        error: (err: any) => {
          this.error = 'Không thể tải thông tin cá nhân';
          if (err.status === 401 || err.status === 403) {
            this.authService.logout();
          }
        }
      });
    }
    if (this.user && !this.original) {
      this.original = JSON.parse(JSON.stringify(this.user));
    }
  }

  loadAddresses() {
    this.authService.getAddresses().subscribe({
      next: (data) => this.addresses = data,
      error: () => this.toastr.error('Không thể tải danh sách địa chỉ')
    });
  }

  loadLoyaltyHistory() {
    this.isLoadingLoyalty = true;
    this.authService.getLoyaltyHistory(0, 10).subscribe({
      next: (res) => {
        this.loyaltyHistory = res.content || [];
        this.isLoadingLoyalty = false;
      },
      error: () => {
        this.isLoadingLoyalty = false;
      }
    });
  }

  toggleAddAddress() {
    this.isAddingAddress = !this.isAddingAddress;
    this.newAddress = { receiverName: this.user?.fullName || '', phoneNumber: this.user?.phoneNumber || '', address: '', isDefault: false };
  }

  saveNewAddress() {
    if (!this.newAddress.receiverName || !this.newAddress.phoneNumber || !this.newAddress.address) {
      this.toastr.warning('Vui lòng điền đầy đủ thông tin địa chỉ');
      return;
    }
    this.authService.addAddress(this.newAddress).subscribe({
      next: () => {
        this.toastr.success('Thêm địa chỉ thành công');
        this.isAddingAddress = false;
        this.loadAddresses();
      },
      error: () => this.toastr.error('Thêm địa chỉ thất bại')
    });
  }

  deleteAddress(id?: number) {
    if (!id) return;
    if (confirm('Bạn có chắc chắn muốn xóa địa chỉ này?')) {
      this.authService.deleteAddress(id).subscribe({
        next: () => {
          this.toastr.success('Xóa địa chỉ thành công');
          this.loadAddresses();
        },
        error: () => this.toastr.error('Xóa địa chỉ thất bại')
      });
    }
  }

  setDefaultAddress(id?: number) {
    if (!id) return;
    this.authService.setDefaultAddress(id).subscribe({
      next: () => {
        this.toastr.success('Đã đặt làm địa chỉ mặc định');
        this.loadAddresses();
      },
      error: () => this.toastr.error('Thao tác thất bại')
    });
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
    this.router.navigate(['/dashboard']);
  }

  switchToSeller() {
    if (this.canGoToShop()) {
      this.router.navigate(['/seller/dashboard']);
    } else {
      this.toastr.warning('Bạn chưa được duyệt bán hàng. Vui lòng đăng ký hoặc chờ admin duyệt.');
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
