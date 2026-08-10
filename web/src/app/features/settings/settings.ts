import { Component, effect, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { RealtimeStore } from '../../core/realtime.store';

const PREF_KEY = 'cochat.desktopNotifications';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.html',
  styleUrl: './settings.scss',
})
export class Settings {
  desktopNotifications = signal(localStorage.getItem(PREF_KEY) === 'true');
  permission = signal(typeof Notification !== 'undefined' ? Notification.permission : 'denied');
  private lastCount = 0;

  constructor(public auth: AuthService, private store: RealtimeStore, private router: Router) {
    this.lastCount = this.store.notifications().length;
    effect(() => {
      const notifications = this.store.notifications();
      if (notifications.length > this.lastCount && this.desktopNotifications() && this.permission() === 'granted') {
        const latest = notifications[0];
        new Notification(latest.title, { body: latest.body });
      }
      this.lastCount = notifications.length;
    });
  }

  async toggleDesktopNotifications() {
    if (!this.desktopNotifications()) {
      const result = await Notification.requestPermission();
      this.permission.set(result);
      if (result !== 'granted') return;
    }
    const next = !this.desktopNotifications();
    this.desktopNotifications.set(next);
    localStorage.setItem(PREF_KEY, String(next));
  }

  async logout() {
    this.store.stop();
    await this.auth.logout();
    this.router.navigate(['/login']);
  }
}
