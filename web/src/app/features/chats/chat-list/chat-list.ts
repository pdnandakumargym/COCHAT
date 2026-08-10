import { Component } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { RealtimeStore } from '../../../core/realtime.store';
import { ChatSummary } from '../../../core/models';
import { Avatar } from '../../../shared/components/avatar/avatar';

@Component({
  selector: 'app-chat-list',
  imports: [RouterLink, Avatar, DatePipe],
  templateUrl: './chat-list.html',
  styleUrl: './chat-list.scss',
})
export class ChatList {
  constructor(public store: RealtimeStore, private router: Router) {}

  get sortedChats(): ChatSummary[] {
    return this.store.chats();
  }

  displayName(chat: ChatSummary): string {
    return chat.type === 'group' ? chat.name || 'Group' : chat.peer?.fullName || 'Unknown';
  }

  displayAvatar(chat: ChatSummary): string {
    return chat.type === 'group' ? chat.avatar || '' : chat.peer?.profilePicture || '';
  }

  displayStatus(chat: ChatSummary) {
    return chat.type === 'private' ? chat.peer?.status ?? 'offline' : null;
  }

  previewText(chat: ChatSummary): string {
    if (!chat.lastMessage) return 'No messages yet';
    const lm = chat.lastMessage;
    if (lm.type === 'text') return lm.text;
    return `Sent a ${lm.type}`;
  }

  open(chatId: string) {
    this.router.navigate(['/chats', chatId]);
  }
}
