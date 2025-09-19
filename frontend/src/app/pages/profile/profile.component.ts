import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, UserProfile } from '../../services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  user: UserProfile | null = null;
  private original: UserProfile | null = null;
  isEditing = false;
  error = '';
  success = '';

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit() {
    this.loadUserProfile();
  }

  loadUserProfile() {
    console.log('Loading user profile...');
    this.user = this.authService.getUser();
    console.log('User from localStorage:', this.user);
    
    if (!this.user) {
      console.log('No user in localStorage, fetching from API...');
      this.authService.getProfile().subscribe({
        next: (profile) => {
          console.log('Profile fetched from API:', profile);
          this.user = profile;
          this.authService.saveUser(profile);
          this.original = JSON.parse(JSON.stringify(profile));
        },
        error: (err) => {
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
      next: (updatedProfile) => {
        this.user = updatedProfile;
        this.authService.saveUser(updatedProfile);
        this.isEditing = false;
        this.success = 'Cập nhật thông tin thành công!';
        this.error = '';
        this.original = JSON.parse(JSON.stringify(updatedProfile));
      },
      error: (err) => {
        this.error = err?.error?.message || 'Cập nhật thất bại';
        console.error('Error updating profile:', err);
      }
    });
  }

  cancelEdit() {
    this.loadUserProfile();
    this.isEditing = false;
    this.error = '';
    this.success = '';
  }

  getGenderText(gender: string): string {
    switch (gender) {
      case 'MALE': return 'Nam';
      case 'FEMALE': return 'Nữ';
      case 'OTHER': return 'Khác';
      default: return 'Chưa cập nhật';
    }
  }

  goBack() {
    this.router.navigate(['/admin/dashboard']);
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
}
