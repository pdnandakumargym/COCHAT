import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UsersService } from '../../../core/users.service';
import { GroupsService } from '../../../core/groups.service';
import { RealtimeStore } from '../../../core/realtime.store';
import { User } from '../../../core/models';
import { Avatar } from '../../../shared/components/avatar/avatar';

@Component({
  selector: 'app-create-group',
  imports: [FormsModule, RouterLink, Avatar],
  templateUrl: './create-group.html',
  styleUrl: './create-group.scss',
})
export class CreateGroup implements OnInit {
  name = '';
  query = '';
  members = signal<User[]>([]);
  selectedIds = signal<Set<string>>(new Set());
  loading = signal(true);
  saving = signal(false);
  error = signal('');

  constructor(
    private usersApi: UsersService,
    private groupsApi: GroupsService,
    private store: RealtimeStore,
    private router: Router
  ) {}

  async ngOnInit() {
    this.members.set(await this.usersApi.list());
    this.loading.set(false);
  }

  async onQueryChange() {
    this.members.set(await this.usersApi.list(this.query.trim()));
  }

  toggle(id: string) {
    this.selectedIds.update((set) => {
      const next = new Set(set);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  isSelected(id: string): boolean {
    return this.selectedIds().has(id);
  }

  get selectedCount(): number {
    return this.selectedIds().size;
  }

  async create() {
    this.error.set('');
    if (!this.name.trim()) {
      this.error.set('Give your group a name.');
      return;
    }
    if (this.selectedCount === 0) {
      this.error.set('Add at least one team member.');
      return;
    }
    this.saving.set(true);
    try {
      const group = await this.groupsApi.create(this.name.trim(), [...this.selectedIds()]);
      this.router.navigate(['/chats', group.id]);
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Could not create group.');
    } finally {
      this.saving.set(false);
    }
  }
}
