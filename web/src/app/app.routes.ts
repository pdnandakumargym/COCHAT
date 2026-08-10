import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./features/shell/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'chats' },
      {
        path: 'chats',
        loadComponent: () => import('./features/chats/chat-list/chat-list').then((m) => m.ChatList),
      },
      {
        path: 'chats/:id',
        loadComponent: () => import('./features/chats/chat-thread/chat-thread').then((m) => m.ChatThread),
      },
      {
        path: 'groups/new',
        loadComponent: () => import('./features/chats/create-group/create-group').then((m) => m.CreateGroup),
      },
      {
        path: 'groups/:id/info',
        loadComponent: () => import('./features/chats/group-info/group-info').then((m) => m.GroupInfoPage),
      },
      {
        path: 'team',
        loadComponent: () => import('./features/team/team-list').then((m) => m.TeamList),
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/profile/profile-view/profile-view').then((m) => m.ProfileView),
      },
      {
        path: 'profile/edit',
        loadComponent: () => import('./features/profile/profile-edit/profile-edit').then((m) => m.ProfileEdit),
      },
      {
        path: 'notifications',
        loadComponent: () => import('./features/notifications/notifications-list').then((m) => m.NotificationsList),
      },
      {
        path: 'settings',
        loadComponent: () => import('./features/settings/settings').then((m) => m.Settings),
      },
    ],
  },
  { path: '**', redirectTo: 'chats' },
];
