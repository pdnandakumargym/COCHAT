export type PresenceStatus = 'online' | 'away' | 'offline';

export interface User {
  id: string;
  fullName: string;
  email: string | null;
  mobile: string | null;
  designation: string;
  profilePicture: string;
  status: PresenceStatus;
  lastSeen: string;
}

export interface AuthResponse {
  user: User;
  accessToken: string;
  refreshToken: string;
}

export type MessageType = 'text' | 'image' | 'video' | 'file';

export interface Attachment {
  url: string;
  fileName: string;
  mimeType: string;
  size: number;
}

export interface Message {
  _id: string;
  chat: string;
  sender: { _id: string; fullName: string; profilePicture: string } | string;
  type: MessageType;
  text: string;
  attachment?: Attachment;
  readBy: string[];
  systemEvent: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LastMessage {
  text: string;
  senderId: string;
  type: MessageType;
  createdAt: string;
}

export interface ChatPeer {
  id: string;
  fullName: string;
  profilePicture: string;
  designation: string;
  status: PresenceStatus;
}

export interface ChatSummary {
  id: string;
  type: 'private' | 'group';
  lastMessage: LastMessage | null;
  unreadCount: number;
  updatedAt: string;
  name?: string;
  avatar?: string;
  memberCount?: number;
  peer?: ChatPeer;
}

export interface GroupMember {
  id: string;
  role: 'admin' | 'member';
  fullName: string;
  profilePicture: string;
  designation: string;
  status: PresenceStatus;
}

export interface GroupInfo {
  id: string;
  type: 'group';
  name: string;
  avatar: string;
  createdBy: string;
  createdAt: string;
  members: GroupMember[];
}

export interface AppNotification {
  _id: string;
  user: string;
  type:
    | 'private_message'
    | 'group_message'
    | 'group_created'
    | 'member_added'
    | 'member_removed'
    | 'media_shared';
  title: string;
  body: string;
  chat?: string;
  actor?: { _id: string; fullName: string; profilePicture: string };
  read: boolean;
  createdAt: string;
}
