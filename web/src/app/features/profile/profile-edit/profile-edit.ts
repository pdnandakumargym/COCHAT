import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { UsersService } from '../../../core/users.service';
import { Avatar } from '../../../shared/components/avatar/avatar';

@Component({
  selector: 'app-profile-edit',
  imports: [FormsModule, RouterLink, Avatar],
  templateUrl: './profile-edit.html',
  styleUrl: './profile-edit.scss',
})
export class ProfileEdit implements OnInit {
  fullName = '';
  designation = '';
  saving = signal(false);
  uploadingAvatar = signal(false);
  success = signal('');
  error = signal('');

  constructor(public auth: AuthService, private usersApi: UsersService, private router: Router) {}

  ngOnInit(): void {
    const user = this.auth.currentUser();
    this.fullName = user?.fullName || '';
    this.designation = user?.designation || '';
  }

  async save() {
    this.error.set('');
    this.success.set('');
    if (!this.fullName.trim()) {
      this.error.set('Full name is required.');
      return;
    }
    this.saving.set(true);
    try {
      const user = await this.usersApi.updateMe({
        fullName: this.fullName.trim(),
        designation: this.designation.trim(),
      });
      this.auth.updateStoredUser(user);
      this.success.set('Profile updated.');
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Could not update profile.');
    } finally {
      this.saving.set(false);
    }
  }

  async onAvatarSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.uploadingAvatar.set(true);
    try {
      const user = await this.usersApi.uploadAvatar(file);
      this.auth.updateStoredUser(user);
    } finally {
      this.uploadingAvatar.set(false);
    }
  }
}
