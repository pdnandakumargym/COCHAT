import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  identifier = '';
  password = '';
  loading = signal(false);
  error = signal('');

  constructor(private auth: AuthService, private router: Router) {}

  async submit() {
    this.error.set('');
    if (!this.identifier.trim() || !this.password) {
      this.error.set('Enter your email/mobile and password.');
      return;
    }
    this.loading.set(true);
    try {
      await this.auth.login(this.identifier.trim(), this.password);
      this.router.navigate(['/chats']);
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Login failed. Check your credentials.');
    } finally {
      this.loading.set(false);
    }
  }
}
