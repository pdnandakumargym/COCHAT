import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UsersService } from '../../core/users.service';
import { ChatsService } from '../../core/chats.service';
import { RealtimeStore } from '../../core/realtime.store';
import { User } from '../../core/models';
import { Avatar } from '../../shared/components/avatar/avatar';

@Component({
  selector: 'app-team-list',
  imports: [FormsModule, Avatar],
  templateUrl: './team-list.html',
  styleUrl: './team-list.scss',
})
export class TeamList implements OnInit {
  members = signal<User[]>([]);
  loading = signal(true);
  query = '';
  private searchTimer?: ReturnType<typeof setTimeout>;

  constructor(
    private usersApi: UsersService,
    private chatsApi: ChatsService,
    private store: RealtimeStore,
    private router: Router
  ) {}

  async ngOnInit() {
    await this.search();
  }

  onQueryChange() {
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => this.search(), 250);
  }

  async search() {
    this.loading.set(true);
    const users = await this.usersApi.list(this.query.trim());
    this.members.set(users);
    this.loading.set(false);
  }

  status(member: User) {
    return this.store.presence()[member.id] ?? member.status;
  }

  async startChat(member: User) {
    const chat = await this.chatsApi.openPrivate(member.id);
    this.store.chats.update((list) => {
      if (list.some((c) => c.id === chat.id)) return list;
      return [chat, ...list];
    });
    this.router.navigate(['/chats', chat.id]);
  }
}
