import { Injectable, computed, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { ChatsService } from './chats.service';
import { NotificationsService } from './notifications.service';
import { SocketService } from './socket.service';
import { AppNotification, ChatSummary, GroupInfo, Message, PresenceStatus } from './models';

interface TypingState {
  chatId: string;
  userId: string;
  isTyping: boolean;
}

@Injectable({ providedIn: 'root' })
export class RealtimeStore {
  readonly chats = signal<ChatSummary[]>([]);
  readonly presence = signal<Record<string, PresenceStatus>>({});
  readonly notifications = signal<AppNotification[]>([]);
  readonly typing = signal<Record<string, Set<string>>>({});
  readonly unreadNotificationCount = computed(() => this.notifications().filter((n) => !n.read).length);
  readonly totalUnreadChats = computed(() => this.chats().reduce((sum, c) => sum + c.unreadCount, 0));

  /** Emits the latest live message per chat so open threads can append without a refetch. */
  readonly lastIncomingMessage = signal<{ chatId: string; message: Message } | null>(null);
  readonly lastGroupUpdate = signal<{ chat: GroupInfo; action: string } | null>(null);

  private started = false;

  constructor(
    private socket: SocketService,
    private auth: AuthService,
    private chatsApi: ChatsService,
    private notificationsApi: NotificationsService
  ) {}

  async start(): Promise<void> {
    if (this.started) return;
    this.started = true;

    const token = this.auth.accessToken;
    if (!token) return;
    this.socket.connect(token);

    // Subscribe before awaiting anything else — the server broadcasts this
    // socket's own presence:update the moment it connects, so registering
    // listeners after an `await` risks missing events that arrive in between.
    this.socket.on<{ userId: string; status: PresenceStatus }>('presence:update').subscribe(({ userId, status }) => {
      this.presence.update((map) => ({ ...map, [userId]: status }));
    });

    this.socket.on<{ message: Message; chatId: string }>('message:new').subscribe(({ message, chatId }) => {
      this.lastIncomingMessage.set({ chatId, message });
    });

    this.socket.on<{ chat: ChatSummary }>('chat:updated').subscribe(({ chat }) => {
      this.chats.update((list) => {
        const idx = list.findIndex((c) => c.id === chat.id);
        const next = idx >= 0 ? [...list.slice(0, idx), chat, ...list.slice(idx + 1)] : [chat, ...list];
        return next.sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());
      });
    });

    this.socket.on<{ chat: GroupInfo; action: string }>('group:updated').subscribe(({ chat, action }) => {
      this.lastGroupUpdate.set({ chat, action });
      if (action === 'created') {
        this.chatsApi.list().then((chats) => this.chats.set(chats));
      }
    });

    this.socket.on<AppNotification>('notification:new').subscribe((notification) => {
      this.notifications.update((list) => [notification, ...list]);
    });

    this.socket.on<TypingState>('typing:update').subscribe(({ chatId, userId, isTyping }) => {
      this.typing.update((map) => {
        const set = new Set(map[chatId] ?? []);
        if (isTyping) set.add(userId);
        else set.delete(userId);
        return { ...map, [chatId]: set };
      });
    });

    const [chats, notifications] = await Promise.all([this.chatsApi.list(), this.notificationsApi.list()]);
    this.chats.set(chats);
    this.notifications.set(notifications);

    const presenceSnapshot: Record<string, PresenceStatus> = {};
    for (const chat of chats) {
      if (chat.peer) presenceSnapshot[chat.peer.id] = chat.peer.status;
    }
    // Merge under whatever presence:update events already arrived via the socket
    // (those are newer than this REST snapshot) rather than overwriting them.
    this.presence.update((live) => ({ ...presenceSnapshot, ...live }));
  }

  markChatReadLocally(chatId: string) {
    this.chats.update((list) => list.map((c) => (c.id === chatId ? { ...c, unreadCount: 0 } : c)));
  }

  stop(): void {
    this.socket.disconnect();
    this.started = false;
    this.chats.set([]);
    this.presence.set({});
    this.notifications.set([]);
  }
}
