import { Component, OnInit, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { GroupsService } from '../../../core/groups.service';
import { UsersService } from '../../../core/users.service';
import { RealtimeStore } from '../../../core/realtime.store';
import { GroupInfo as GroupInfoModel, User } from '../../../core/models';
import { Avatar } from '../../../shared/components/avatar/avatar';

@Component({
  selector: 'app-group-info',
  imports: [FormsModule, RouterLink, Avatar],
  templateUrl: './group-info.html',
  styleUrl: './group-info.scss',
})
export class GroupInfoPage implements OnInit {
  groupId = '';
  group = signal<GroupInfoModel | null>(null);
  loading = signal(true);
  editingName = signal(false);
  nameDraft = '';
  showAddMembers = signal(false);
  candidateMembers = signal<User[]>([]);
  candidateQuery = '';
  error = signal('');

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private groupsApi: GroupsService,
    private usersApi: UsersService,
    public auth: AuthService,
    private store: RealtimeStore
  ) {
    effect(() => {
      const update = this.store.lastGroupUpdate();
      if (update && update.chat.id === this.groupId) {
        this.group.set(update.chat);
      }
    });
  }

  async ngOnInit() {
    this.groupId = this.route.snapshot.paramMap.get('id') || '';
    this.group.set(await this.groupsApi.get(this.groupId));
    this.loading.set(false);
  }

  get isAdmin(): boolean {
    const uid = this.auth.currentUser()?.id;
    return !!this.group()?.members.find((m) => m.id === uid && m.role === 'admin');
  }

  status(memberId: string, fallback: string) {
    return this.store.presence()[memberId] ?? fallback;
  }

  startEditName() {
    this.nameDraft = this.group()?.name || '';
    this.editingName.set(true);
  }

  async saveName() {
    if (!this.nameDraft.trim()) return;
    const updated = await this.groupsApi.updateInfo(this.groupId, this.nameDraft.trim());
    this.group.set(updated);
    this.editingName.set(false);
  }

  async onAvatarSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const updated = await this.groupsApi.updateAvatar(this.groupId, file);
    this.group.set(updated);
  }

  async openAddMembers() {
    this.showAddMembers.set(true);
    this.candidateQuery = '';
    await this.refreshCandidates();
  }

  async refreshCandidates() {
    const users = await this.usersApi.list(this.candidateQuery.trim());
    const existingIds = new Set(this.group()?.members.map((m) => m.id));
    this.candidateMembers.set(users.filter((u) => !existingIds.has(u.id)));
  }

  async addMember(userId: string) {
    const updated = await this.groupsApi.addMembers(this.groupId, [userId]);
    this.group.set(updated);
    await this.refreshCandidates();
  }

  async removeMember(userId: string) {
    this.error.set('');
    try {
      await this.groupsApi.removeMember(this.groupId, userId);
      this.group.update((g) => (g ? { ...g, members: g.members.filter((m) => m.id !== userId) } : g));
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Could not remove member.');
    }
  }

  async leaveGroup() {
    await this.groupsApi.leave(this.groupId);
    this.store.chats.update((list) => list.filter((c) => c.id !== this.groupId));
    this.router.navigate(['/chats']);
  }
}
