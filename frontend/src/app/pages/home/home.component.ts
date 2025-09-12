import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService, UserProfile } from '../../services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  private auth = inject(AuthService);
  profile: UserProfile | null = null;
  loading = false;

  ngOnInit() {
    this.loadProfile();
  }

  logout() {
    this.auth.logout();
  }

  loadProfile() {
    this.loading = true;
    
    // Lấy thông tin user từ localStorage trước
    this.profile = this.auth.getUser();
    if (this.profile) {
      this.loading = false;
      return;
    }
    
    // Nếu không có trong localStorage, gọi API
    this.auth.getProfile().subscribe({
      next: (res: UserProfile) => {
        this.profile = res;
        this.auth.saveUser(res);
        this.loading = false;
        console.log('Profile loaded successfully:', res);
      },
      error: (error: any) => {
        this.loading = false;
        console.error('Failed to load profile:', error);
        console.error('Error status:', error.status);
        console.error('Error headers:', error.headers);
        console.error('Error body:', error.error);
        alert(`Lỗi ${error.status}: ${error.message}`);
      }
    });
  }

  saveProfile() {
    if (!this.profile) return;
    this.auth.updateProfile(this.profile).subscribe({
      next: (res: UserProfile) => { 
        this.profile = res;
        this.auth.saveUser(res);
      },
      error: (error: any) => {
        console.error('Error updating profile:', error);
        alert('Cập nhật thất bại');
      }
    });
  }
}
