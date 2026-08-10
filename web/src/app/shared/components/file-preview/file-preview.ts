import { Component, Input } from '@angular/core';
import { Attachment, MessageType } from '../../../core/models';

@Component({
  selector: 'app-file-preview',
  templateUrl: './file-preview.html',
  styleUrl: './file-preview.scss',
})
export class FilePreview {
  @Input({ required: true }) attachment!: Attachment;
  @Input({ required: true }) type!: MessageType;

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
