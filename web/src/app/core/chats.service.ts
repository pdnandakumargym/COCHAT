import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ChatSummary, Message } from './models';

@Injectable({ providedIn: 'root' })
export class ChatsService {
  constructor(private http: HttpClient) {}

  list(): Promise<ChatSummary[]> {
    return firstValueFrom(this.http.get<{ chats: ChatSummary[] }>(`${environment.apiUrl}/chats`)).then(
      (r) => r.chats
    );
  }

  openPrivate(userId: string): Promise<ChatSummary> {
    return firstValueFrom(
      this.http.post<{ chat: ChatSummary }>(`${environment.apiUrl}/chats/private`, { userId })
    ).then((r) => r.chat);
  }

  messages(chatId: string, before?: string): Promise<Message[]> {
    const params: Record<string, string> = {};
    if (before) params['before'] = before;
    return firstValueFrom(
      this.http.get<{ messages: Message[] }>(`${environment.apiUrl}/chats/${chatId}/messages`, { params })
    ).then((r) => r.messages);
  }

  sendMessage(chatId: string, payload: { text?: string; file?: File }): Promise<Message> {
    const form = new FormData();
    if (payload.text) form.append('text', payload.text);
    if (payload.file) form.append('file', payload.file);
    return firstValueFrom(
      this.http.post<{ message: Message }>(`${environment.apiUrl}/chats/${chatId}/messages`, form)
    ).then((r) => r.message);
  }

  markRead(chatId: string): Promise<void> {
    return firstValueFrom(this.http.post<void>(`${environment.apiUrl}/chats/${chatId}/read`, {}));
  }
}
