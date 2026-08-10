import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth.service';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  identifierType: 'email' | 'mobile' = 'email';
  fullName = '';
  email = '';
  mobile = '';
  designation = '';
  password = '';
  loading = signal(false);
  error = signal('');

  constructor(private auth: AuthService, private router: Router) {}

  async submit() {
    this.error.set('');
    if (!this.fullName.trim() || this.password.length < 6) {
      this.error.set('Full name is required and password must be at least 6 characters.');
      return;
    }
    if (this.identifierType === 'email' && !this.email.trim()) {
      this.error.set('Enter your email address.');
      return;
    }
    if (this.identifierType === 'mobile' && !this.mobile.trim()) {
      this.error.set('Enter your mobile number.');
      return;
    }

    this.loading.set(true);
    try {
      await this.auth.register({
        fullName: this.fullName.trim(),
        email: this.identifierType === 'email' ? this.email.trim() : undefined,
        mobile: this.identifierType === 'mobile' ? this.mobile.trim() : undefined,
        password: this.password,
        designation: this.designation.trim(),
      });
      this.router.navigate(['/chats']);
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Registration failed.');
    } finally {
      this.loading.set(false);
    }
  }
}
