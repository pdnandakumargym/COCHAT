import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse, User } from './models';

const ACCESS_KEY = 'cochat.accessToken';
const REFRESH_KEY = 'cochat.refreshToken';
const USER_KEY = 'cochat.user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly currentUser = signal<User | null>(this.readStoredUser());

  constructor(private http: HttpClient) {}

  get accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  get refreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  get isLoggedIn(): boolean {
    return !!this.accessToken;
  }

  private readStoredUser(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as User) : null;
  }

  private persist(res: AuthResponse) {
    localStorage.setItem(ACCESS_KEY, res.accessToken);
    localStorage.setItem(REFRESH_KEY, res.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    this.currentUser.set(res.user);
  }

  async register(payload: {
    fullName: string;
    email?: string;
    mobile?: string;
    password: string;
    designation?: string;
  }): Promise<void> {
    const res = await firstValueFrom(this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, payload));
    this.persist(res);
  }

  async login(identifier: string, password: string): Promise<void> {
    const res = await firstValueFrom(
      this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, { identifier, password })
    );
    this.persist(res);
  }

  async refresh(): Promise<string> {
    const refreshToken = this.refreshToken;
    if (!refreshToken) throw new Error('No refresh token');
    const res = await firstValueFrom(
      this.http.post<AuthResponse>(`${environment.apiUrl}/auth/refresh`, { refreshToken })
    );
    this.persist(res);
    return res.accessToken;
  }

  async logout(): Promise<void> {
    const refreshToken = this.refreshToken;
    try {
      await firstValueFrom(this.http.post(`${environment.apiUrl}/auth/logout`, { refreshToken }));
    } catch {
      // best-effort revoke; clear local session regardless
    }
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUser.set(null);
  }

  updateStoredUser(user: User) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.currentUser.set(user);
  }
}
