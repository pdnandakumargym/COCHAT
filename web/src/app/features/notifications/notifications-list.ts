import { Component } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { RealtimeStore } from '../../core/realtime.store';
import { NotificationsService } from '../../core/notifications.service';
import { AppNotification } from '../../core/models';
import { Avatar } from '../../shared/components/avatar/avatar';

const ICONS: Record<AppNotification['type'], string> = {
  private_message: '💬',
  group_message: '👥',
  group_created: '🎉',
  member_added: '➕',
  member_removed: '➖',
  media_shared: '📎',
};

@Component({
  selector: 'app-notifications-list',
  imports: [DatePipe, Avatar],
  templateUrl: './notifications-list.html',
  styleUrl: './notifications-list.scss',
})
export class NotificationsList {
  constructor(public store: RealtimeStore, private notificationsApi: NotificationsService, private router: Router) {}

  icon(type: AppNotification['type']): string {
    return ICONS[type] || '🔔';
  }

  async markAllRead() {
    await this.notificationsApi.markAllRead();
    this.store.notifications.update((list) => list.map((n) => ({ ...n, read: true })));
  }

  async open(notification: AppNotification) {
    if (!notification.read) {
      await this.notificationsApi.markRead(notification._id);
      this.store.notifications.update((list) =>
        list.map((n) => (n._id === notification._id ? { ...n, read: true } : n))
      );
    }
    if (notification.chat) this.router.navigate(['/chats', notification.chat]);
  }
}
