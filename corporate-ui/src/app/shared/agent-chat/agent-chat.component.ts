import { Component, ElementRef, ViewChild, effect, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AgentService } from '../../core/agent.service';
import { MoneyPipe } from '../money.pipe';

/**
 * Right-side slide-out gifting concierge. Mounted once in the app shell so it
 * persists across route changes; all state lives in the root-provided
 * {@link AgentService}.
 */
@Component({
  selector: 'app-agent-chat',
  standalone: true,
  imports: [FormsModule, MoneyPipe],
  templateUrl: './agent-chat.component.html',
  styleUrl: './agent-chat.component.css'
})
export class AgentChatComponent {
  protected agent = inject(AgentService);
  protected draft = '';

  @ViewChild('scroll') private scroll?: ElementRef<HTMLDivElement>;

  constructor() {
    // Keep the transcript pinned to the latest message / activity.
    effect(() => {
      this.agent.messages();
      this.agent.toolActivity();
      this.agent.draft();
      queueMicrotask(() => {
        const el = this.scroll?.nativeElement;
        if (el) el.scrollTop = el.scrollHeight;
      });
    });
  }

  protected submit(): void {
    const text = this.draft;
    this.draft = '';
    void this.agent.send(text);
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.submit();
    }
  }
}
