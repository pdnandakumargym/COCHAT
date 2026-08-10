import { Component, Input } from '@angular/core';
import { Message } from '../../../core/models';
import { FilePreview } from '../file-preview/file-preview';

@Component({
  selector: 'app-message-bubble',
  imports: [FilePreview],
  templateUrl: './message-bubble.html',
  styleUrl: './message-bubble.scss',
})
export class MessageBubble {
  @Input({ required: true }) message!: Message;
  @Input() isOwn = false;
  @Input() showSenderName = false;

  get senderName(): string {
    return typeof this.message.sender === 'string' ? '' : this.message.sender.fullName;
  }

  get senderAvatar(): string {
    return typeof this.message.sender === 'string' ? '' : this.message.sender.profilePicture;
  }

  get time(): string {
    return new Date(this.message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
}
