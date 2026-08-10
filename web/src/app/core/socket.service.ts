import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { io, Socket } from 'socket.io-client';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SocketService {
  private socket: Socket | null = null;

  connect(token: string): void {
    if (this.socket?.connected) return;
    this.socket = io(environment.socketUrl, {
      auth: { token },
    });
  }

  disconnect(): void {
    this.socket?.disconnect();
    this.socket = null;
  }

  emit(event: string, payload: unknown): void {
    this.socket?.emit(event, payload);
  }

  on<T>(event: string): Observable<T> {
    return new Observable<T>((subscriber) => {
      if (!this.socket) return;
      const handler = (data: T) => subscriber.next(data);
      this.socket.on(event, handler);
      return () => this.socket?.off(event, handler);
    });
  }
}
