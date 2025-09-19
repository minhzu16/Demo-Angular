import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
    rememberMe: [false]
  });

  loading = false;
  error: string | null = null;

  submit() {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = null;
    
    const loginData = {
      username: this.form.value.username!,
      password: this.form.value.password!
    };
    
    this.auth.login(loginData).subscribe({
      next: res => {
        console.log('Login successful, saving token and navigating...', res);
        this.auth.saveToken(res.accessToken);
        console.log('Token saved, navigating to dashboard...');
        // Welcome is shown on dashboard after navigation
        
        // Add a small delay to ensure token is saved, then navigate with state
        setTimeout(() => {
          const fullName = (res?.user?.fullName || res?.user?.username || this.form.value.username) as string;
          this.router.navigate(['/admin/dashboard'], { state: { fromLogin: true, fullName } }).then(success => {
            console.log('Navigation result:', success);
            if (!success) {
              console.error('Navigation failed, trying alternative route...');
              this.router.navigate(['/admin/dashboard'], { state: { fromLogin: true, fullName } });
            }
          }).catch(err => {
            console.error('Navigation error:', err);
          });
        }, 100);
        
        this.loading = false;
      },
      error: err => {
        console.error('Login error', err);
        this.error = (err?.error?.message as string) || 'Login failed';
        this.loading = false;
      }
    });
  }

  onSocialLogin(provider: string) {
    console.log(`Login with ${provider}`);
    // Implement social login logic here
  }
}
