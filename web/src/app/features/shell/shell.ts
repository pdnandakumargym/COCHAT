import { Component, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { RealtimeStore } from '../../core/realtime.store';
import { PresenceStatus } from '../../core/models';
import { Avatar } from '../../shared/components/avatar/avatar';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Avatar],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell implements OnInit {
  constructor(public auth: AuthService, public store: RealtimeStore, private router: Router) {}

  ngOnInit(): void {
    this.store.start();
  }

  get myStatus(): PresenceStatus {
    const user = this.auth.currentUser();
    if (!user) return 'offline';
    return this.store.presence()[user.id] ?? user.status;
  }

  async logout() {
    this.store.stop();
    await this.auth.logout();
    this.router.navigate(['/login']);
  }
}
