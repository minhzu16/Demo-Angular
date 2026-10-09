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
  showPassword = false;
  showForgotHint = false;

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  submit() {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = null;

    const loginData = {
      usernameOrEmail: this.form.value.username!,
      password: this.form.value.password!
    };

    this.auth.login(loginData).subscribe({
      next: res => {
        this.auth.saveToken(res.accessToken);
        const fullName = (res?.user?.fullName || res?.user?.username || this.form.value.username) as string;

        const roles = res?.user?.role || 'BUYER';
        const returnUrl = this.router.parseUrl(this.router.url).queryParams['returnUrl'];
        // Only follow same-app absolute paths — never "//host" or "http://…" (open redirect).
        if (typeof returnUrl === 'string' && /^\/(?!\/)/.test(returnUrl)) {
          this.router.navigateByUrl(returnUrl);
        } else if (roles.includes('ADMIN')) {
          this.router.navigate(['/dashboard'], { state: { fromLogin: true, fullName } });
        } else if (roles.includes('SELLER')) {
          this.router.navigate(['/seller/dashboard'], { state: { fromLogin: true, fullName } });
        } else {
          // Regular buyer route
          this.router.navigate(['/home'], { state: { fromLogin: true, fullName } });
        }

        this.loading = false;
      },
      error: err => {
        this.error = (err?.error?.message as string) || err?.message || 'Đăng nhập không thành công. Vui lòng thử lại.';
        this.loading = false;
      }
    });
  }


}
