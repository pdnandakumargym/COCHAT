import { Component, ElementRef, OnDestroy, OnInit, ViewChild, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/auth.service';
import { ChatsService } from '../../../core/chats.service';
import { RealtimeStore } from '../../../core/realtime.store';
import { SocketService } from '../../../core/socket.service';
import { ChatSummary, Message } from '../../../core/models';
import { Avatar } from '../../../shared/components/avatar/avatar';
import { MessageBubble } from '../../../shared/components/message-bubble/message-bubble';

const EMOJI_SET = ['😀', '😂', '😍', '👍', '🙏', '🎉', '❤️', '😢', '😮', '🔥', '✅', '👏'];

@Component({
  selector: 'app-chat-thread',
  imports: [RouterLink, FormsModule, Avatar, MessageBubble],
  templateUrl: './chat-thread.html',
  styleUrl: './chat-thread.scss',
})
export class ChatThread implements OnInit, OnDestroy {
  @ViewChild('scrollArea') scrollArea?: ElementRef<HTMLDivElement>;
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;

  chatId = '';
  chat = signal<ChatSummary | null>(null);
  messages = signal<Message[]>([]);
  draftText = '';
  selectedFile: File | null = null;
  loading = signal(true);
  loadingOlder = signal(false);
  hasMore = signal(true);
  showEmojiPicker = false;
  emojis = EMOJI_SET;

  private sub?: Subscription;
  private typingTimeout?: ReturnType<typeof setTimeout>;
  private isTyping = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private chatsApi: ChatsService,
    public store: RealtimeStore,
    public auth: AuthService,
    private socket: SocketService
  ) {
    effect(() => {
      const incoming = this.store.lastIncomingMessage();
      if (incoming && incoming.chatId === this.chatId) {
        this.appendIfNew(incoming.message);
        const senderId = typeof incoming.message.sender === 'string' ? incoming.message.sender : incoming.message.sender._id;
        if (senderId !== this.auth.currentUser()?.id) {
          this.chatsApi.markRead(this.chatId).catch(() => {});
          this.store.markChatReadLocally(this.chatId);
        }
      }
    });
  }

  ngOnInit(): void {
    this.sub = this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (id) this.loadChat(id);
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    clearTimeout(this.typingTimeout);
  }

  get currentUserId(): string {
    return this.auth.currentUser()?.id || '';
  }

  get typingLabel(): string | null {
    const typists = this.store.typing()[this.chatId];
    if (!typists || typists.size === 0) return null;
    return typists.size === 1 ? 'Typing…' : `${typists.size} people typing…`;
  }

  get headerName(): string {
    const chat = this.chat();
    if (!chat) return '';
    return chat.type === 'group' ? chat.name || 'Group' : chat.peer?.fullName || 'Unknown';
  }

  get headerAvatar(): string {
    const chat = this.chat();
    if (!chat) return '';
    return chat.type === 'group' ? chat.avatar || '' : chat.peer?.profilePicture || '';
  }

  get headerStatus() {
    const chat = this.chat();
    if (!chat || chat.type !== 'private' || !chat.peer) return null;
    return this.store.presence()[chat.peer.id] ?? chat.peer.status;
  }

  get headerSubtitle(): string {
    const chat = this.chat();
    if (!chat) return '';
    if (chat.type === 'group') return `${chat.memberCount ?? 0} members`;
    const status = this.headerStatus;
    return status === 'online' ? 'Online' : status === 'away' ? 'Away' : 'Offline';
  }

  private async loadChat(id: string) {
    this.chatId = id;
    this.loading.set(true);
    this.messages.set([]);
    this.hasMore.set(true);

    let chat = this.store.chats().find((c) => c.id === id) ?? null;
    if (!chat) {
      // direct link / refresh: chat list may not include it yet
      const list = await this.chatsApi.list();
      this.store.chats.set(list);
      chat = list.find((c) => c.id === id) ?? null;
    }
    this.chat.set(chat);

    this.messages.set(await this.chatsApi.messages(id));
    this.loading.set(false);
    this.scrollToBottom();

    await this.chatsApi.markRead(id);
    this.store.markChatReadLocally(id);
  }

  async loadOlder() {
    const current = this.messages();
    if (!current.length || this.loadingOlder()) return;
    this.loadingOlder.set(true);
    const older = await this.chatsApi.messages(this.chatId, current[0]._id);
    if (older.length === 0) this.hasMore.set(false);
    this.messages.set([...older, ...current]);
    this.loadingOlder.set(false);
  }

  private appendIfNew(message: Message) {
    if (this.messages().some((m) => m._id === message._id)) return;
    this.messages.update((list) => [...list, message]);
    this.scrollToBottom();
  }

  onInputChange() {
    if (!this.isTyping) {
      this.isTyping = true;
      this.socket.emit('typing:start', { chatId: this.chatId });
    }
    clearTimeout(this.typingTimeout);
    this.typingTimeout = setTimeout(() => {
      this.isTyping = false;
      this.socket.emit('typing:stop', { chatId: this.chatId });
    }, 1500);
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  clearFile() {
    this.selectedFile = null;
    if (this.fileInput) this.fileInput.nativeElement.value = '';
  }

  addEmoji(emoji: string) {
    this.draftText += emoji;
    this.showEmojiPicker = false;
  }

  async send() {
    const text = this.draftText.trim();
    if (!text && !this.selectedFile) return;

    const file = this.selectedFile ?? undefined;
    this.draftText = '';
    this.clearFile();
    clearTimeout(this.typingTimeout);
    this.isTyping = false;
    this.socket.emit('typing:stop', { chatId: this.chatId });

    const message = await this.chatsApi.sendMessage(this.chatId, { text: text || undefined, file });
    this.appendIfNew(message);
  }

  private scrollToBottom() {
    setTimeout(() => {
      const el = this.scrollArea?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    }, 0);
  }

  goToGroupInfo() {
    if (this.chat()?.type === 'group') this.router.navigate(['/groups', this.chatId, 'info']);
  }

  isOwnMessage(message: Message): boolean {
    const senderId = typeof message.sender === 'string' ? message.sender : message.sender._id;
    return senderId === this.currentUserId;
  }
}
