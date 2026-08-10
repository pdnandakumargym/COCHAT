import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { RealtimeStore } from '../../../core/realtime.store';
import { PresenceStatus } from '../../../core/models';
import { Avatar } from '../../../shared/components/avatar/avatar';

@Component({
  selector: 'app-profile-view',
  imports: [RouterLink, Avatar],
  templateUrl: './profile-view.html',
  styleUrl: './profile-view.scss',
})
export class ProfileView {
  constructor(public auth: AuthService, private store: RealtimeStore) {}

  get status(): PresenceStatus {
    const user = this.auth.currentUser();
    if (!user) return 'offline';
    return this.store.presence()[user.id] ?? user.status;
  }
}
