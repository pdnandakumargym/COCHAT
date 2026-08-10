import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { AppNotification } from './models';

@Injectable({ providedIn: 'root' })
export class NotificationsService {
  constructor(private http: HttpClient) {}

  list(): Promise<AppNotification[]> {
    return firstValueFrom(
      this.http.get<{ notifications: AppNotification[] }>(`${environment.apiUrl}/notifications`)
    ).then((r) => r.notifications);
  }

  markRead(id: string): Promise<void> {
    return firstValueFrom(this.http.post<void>(`${environment.apiUrl}/notifications/${id}/read`, {}));
  }

  markAllRead(): Promise<void> {
    return firstValueFrom(this.http.post<void>(`${environment.apiUrl}/notifications/read-all`, {}));
  }
}
