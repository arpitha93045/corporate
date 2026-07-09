import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AgentChatMessage, DraftCart } from '../models/models';
import { CartService } from './cart.service';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';

const CHAT_URL = '/api/agent/chat';

/** Human-friendly labels for the transient "the agent is doing X" line. */
export const TOOL_LABELS: Record<string, string> = {
  search_products: 'Searching the catalog…',
  get_product: 'Checking a product…',
  estimate_total: 'Pricing your selection…',
  create_draft_cart: 'Building your cart…',
  create_enquiry: 'Passing this to our team…'
};

/** Callback bag for a single streamed chat turn. Lets a caller layer its own
 *  state on top of the shared SSE transport without touching the drawer. */
export interface AgentStreamHandlers {
  onMessage(text: string): void;
  onTool(name: string): void;
  onDraft(draft: DraftCart): void;
  onError(message: string): void;
  onDone(): void;
}

/**
 * Client-side store + transport for the gifting concierge. Conversation history is
 * held here (the backend chat endpoint is stateless) and posted in full on each turn.
 * The SSE response is read with fetch()+ReadableStream because Angular's HttpClient
 * cannot surface an event-stream body incrementally.
 */
@Injectable({ providedIn: 'root' })
export class AgentService {
  private cart = inject(CartService);
  private api = inject(ApiService);
  private auth = inject(AuthService);

  private readonly _open = signal(false);
  private readonly _messages = signal<AgentChatMessage[]>([]);
  private readonly _toolActivity = signal<string | null>(null);
  private readonly _draft = signal<DraftCart | null>(null);
  private readonly _streaming = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly open = this._open.asReadonly();
  readonly messages = this._messages.asReadonly();
  readonly toolActivity = this._toolActivity.asReadonly();
  readonly draft = this._draft.asReadonly();
  readonly streaming = this._streaming.asReadonly();
  readonly error = this._error.asReadonly();

  openDrawer(): void { this._open.set(true); }
  close(): void { this._open.set(false); }
  toggle(): void { this._open.update(v => !v); }

  reset(): void {
    this._messages.set([]);
    this._draft.set(null);
    this._error.set(null);
    this._toolActivity.set(null);
  }

  async send(text: string): Promise<void> {
    const trimmed = text.trim();
    if (!trimmed || this._streaming()) return;

    this._error.set(null);
    this._draft.set(null);
    this._messages.update(m => [...m, { role: 'user', content: trimmed }]);
    this._streaming.set(true);
    this._toolActivity.set(null);

    const history = this._messages();
    try {
      await this.streamChat(history, {
        onMessage: t => this.appendAssistantText(t),
        onTool: name => this._toolActivity.set(TOOL_LABELS[name] ?? 'Working…'),
        onDraft: d => this._draft.set(d),
        onError: msg => this._error.set(msg),
        onDone: () => this._toolActivity.set(null)
      });
    } finally {
      this._streaming.set(false);
      this._toolActivity.set(null);
    }
  }

  /**
   * Shared SSE transport: POST the full message history to /api/agent/chat and
   * dispatch each event to the handler bag. Stateless — callers own their state.
   * Used by both the drawer (send) and the gift-plan page.
   */
  async streamChat(messages: AgentChatMessage[], h: AgentStreamHandlers): Promise<void> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    const token = this.auth.token();
    if (token) headers['Authorization'] = `Bearer ${token}`;

    try {
      const res = await fetch(CHAT_URL, {
        method: 'POST',
        headers,
        body: JSON.stringify({ messages })
      });

      if (!res.ok || !res.body) {
        h.onError(res.status === 503
          ? "The gifting assistant isn't available right now."
          : 'Sorry, something went wrong. Please try again.');
        return;
      }

      await this.consumeStream(res.body, h);
    } catch {
      h.onError('Could not reach the assistant. Check your connection and try again.');
    }
  }

  /**
   * Adopt a draft's lines into the client cart, resolving each slug to a Product.
   * Defaults to the drawer's own draft; the gift-plan page passes its own.
   */
  async adoptDraft(draft: DraftCart | null = this._draft()): Promise<void> {
    if (!draft || draft.lines.length === 0) return;

    let added = 0;
    const missing: string[] = [];
    for (const line of draft.lines) {
      try {
        const product = await firstValueFrom(this.api.product(line.productSlug));
        this.cart.add(product, line.quantity);
        added += line.quantity;
      } catch {
        missing.push(line.productName);
      }
    }

    // Only clear the drawer's signal when we adopted the drawer's own draft.
    if (draft === this._draft()) this._draft.set(null);
    let note = `Added ${added} item${added === 1 ? '' : 's'} to your cart.`;
    if (missing.length) {
      note += ` Couldn't add: ${missing.join(', ')} (no longer available).`;
    }
    this._messages.update(m => [...m, { role: 'assistant', content: note }]);
  }

  /** Reads an SSE body (event:/data: frames) and dispatches by event name. */
  private async consumeStream(body: ReadableStream<Uint8Array>, h: AgentStreamHandlers): Promise<void> {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    for (;;) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      // Frames are separated by a blank line.
      let sep: number;
      while ((sep = buffer.indexOf('\n\n')) !== -1) {
        const frame = buffer.slice(0, sep);
        buffer = buffer.slice(sep + 2);
        this.handleFrame(frame, h);
      }
    }
  }

  private handleFrame(frame: string, h: AgentStreamHandlers): void {
    let event = 'message';
    const dataLines: string[] = [];
    for (const raw of frame.split('\n')) {
      const line = raw.trimEnd();
      if (line.startsWith('event:')) event = line.slice(6).trim();
      else if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart());
    }
    if (dataLines.length === 0) return;

    let data: any;
    try {
      data = JSON.parse(dataLines.join('\n'));
    } catch {
      return;
    }

    switch (event) {
      case 'message':
        if (data.text) h.onMessage(data.text);
        break;
      case 'tool':
        h.onTool(data.name);
        break;
      case 'draft_cart':
        h.onDraft(data as DraftCart);
        break;
      case 'error':
        h.onError(data.message ?? 'Something went wrong.');
        break;
      case 'done':
        h.onDone();
        break;
    }
  }

  /** Coalesce streamed text into the trailing assistant bubble (a turn may emit several). */
  private appendAssistantText(text: string): void {
    if (!text) return;
    this._toolActivity.set(null);
    this._messages.update(m => {
      const last = m[m.length - 1];
      if (last && last.role === 'assistant') {
        const next = [...m];
        next[next.length - 1] = { role: 'assistant', content: last.content + text };
        return next;
      }
      return [...m, { role: 'assistant', content: text }];
    });
  }
}
