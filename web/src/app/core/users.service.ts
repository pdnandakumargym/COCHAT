import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { User } from './models';

@Injectable({ providedIn: 'root' })
export class UsersService {
  constructor(private http: HttpClient) {}

  me(): Promise<User> {
    return firstValueFrom(this.http.get<{ user: User }>(`${environment.apiUrl}/users/me`)).then((r) => r.user);
  }

  updateMe(payload: { fullName?: string; designation?: string }): Promise<User> {
    return firstValueFrom(this.http.patch<{ user: User }>(`${environment.apiUrl}/users/me`, payload)).then(
      (r) => r.user
    );
  }

  uploadAvatar(file: File): Promise<User> {
    const form = new FormData();
    form.append('avatar', file);
    return firstValueFrom(this.http.post<{ user: User }>(`${environment.apiUrl}/users/me/avatar`, form)).then(
      (r) => r.user
    );
  }

  list(q = ''): Promise<User[]> {
    return firstValueFrom(this.http.get<{ users: User[] }>(`${environment.apiUrl}/users`, { params: { q } })).then(
      (r) => r.users
    );
  }

  get(id: string): Promise<User> {
    return firstValueFrom(this.http.get<{ user: User }>(`${environment.apiUrl}/users/${id}`)).then((r) => r.user);
  }
}
