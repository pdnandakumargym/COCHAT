import { Component, Input } from '@angular/core';
import { PresenceStatus } from '../../../core/models';

@Component({
  selector: 'app-avatar',
  templateUrl: './avatar.html',
  styleUrl: './avatar.scss',
})
export class Avatar {
  @Input() src = '';
  @Input() name = '';
  @Input() status: PresenceStatus | null = null;
  @Input() size = 40;

  get initials(): string {
    const parts = this.name.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) return '?';
    if (parts.length === 1) return parts[0][0].toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }
}
