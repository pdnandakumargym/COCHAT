import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { GroupInfo } from './models';

@Injectable({ providedIn: 'root' })
export class GroupsService {
  constructor(private http: HttpClient) {}

  create(name: string, memberIds: string[]): Promise<GroupInfo> {
    return firstValueFrom(
      this.http.post<{ group: GroupInfo }>(`${environment.apiUrl}/groups`, { name, memberIds })
    ).then((r) => r.group);
  }

  get(id: string): Promise<GroupInfo> {
    return firstValueFrom(this.http.get<{ group: GroupInfo }>(`${environment.apiUrl}/groups/${id}`)).then(
      (r) => r.group
    );
  }

  updateInfo(id: string, name: string): Promise<GroupInfo> {
    const form = new FormData();
    form.append('name', name);
    return firstValueFrom(this.http.patch<{ group: GroupInfo }>(`${environment.apiUrl}/groups/${id}`, form)).then(
      (r) => r.group
    );
  }

  updateAvatar(id: string, file: File): Promise<GroupInfo> {
    const form = new FormData();
    form.append('avatar', file);
    return firstValueFrom(this.http.patch<{ group: GroupInfo }>(`${environment.apiUrl}/groups/${id}`, form)).then(
      (r) => r.group
    );
  }

  addMembers(id: string, memberIds: string[]): Promise<GroupInfo> {
    return firstValueFrom(
      this.http.post<{ group: GroupInfo }>(`${environment.apiUrl}/groups/${id}/members`, { memberIds })
    ).then((r) => r.group);
  }

  removeMember(id: string, userId: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${environment.apiUrl}/groups/${id}/members/${userId}`));
  }

  leave(id: string): Promise<void> {
    return firstValueFrom(this.http.post<void>(`${environment.apiUrl}/groups/${id}/leave`, {}));
  }
}
