import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AgentService, TOOL_LABELS } from '../core/agent.service';
import { DraftCart, GiftRecipient } from '../models/models';
import { MoneyPipe } from '../shared/money.pipe';

const MAX_RECIPIENTS = 200;

/**
 * Bulk-recipient gift planner. The buyer pastes/uploads a recipient CSV, edits a
 * preview table, and asks the concierge for a consolidated selection. Reuses the
 * shared AgentService SSE transport (POST /api/agent/chat) with the page's own
 * local state; the returned draft cart can be adopted or exported as a PO CSV.
 */
@Component({
  selector: 'app-gift-plan',
  standalone: true,
  imports: [FormsModule, MoneyPipe],
  templateUrl: './gift-plan.component.html',
  styleUrl: './gift-plan.component.css'
})
export class GiftPlanComponent {
  private agent = inject(AgentService);

  protected csvText = '';
  protected occasion = '';
  protected budgetRupees: number | null = null;

  protected recipients = signal<GiftRecipient[]>([]);
  protected parseError = signal<string | null>(null);

  protected streaming = signal(false);
  protected activity = signal<string | null>(null);
  protected assistantText = signal('');
  protected draft = signal<DraftCart | null>(null);
  protected error = signal<string | null>(null);

  protected onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      this.csvText = String(reader.result ?? '');
      this.parse();
    };
    reader.readAsText(file);
  }

  protected parse(): void {
    this.parseError.set(null);
    const rows = this.parseCsv(this.csvText);
    if (rows.length === 0) {
      this.recipients.set([]);
      this.parseError.set('No recipients found. Expected lines like: name, city, notes');
      return;
    }
    if (rows.length > MAX_RECIPIENTS) {
      this.parseError.set(`Too many recipients (${rows.length}). Limit is ${MAX_RECIPIENTS}.`);
      this.recipients.set(rows.slice(0, MAX_RECIPIENTS));
      return;
    }
    this.recipients.set(rows);
  }

  private parseCsv(text: string): GiftRecipient[] {
    const lines = text.split(/\r?\n/).map(l => l.trim()).filter(l => l.length > 0);
    if (lines.length === 0) return [];

    // Tolerate an optional header row.
    const first = lines[0].toLowerCase();
    const start = /\bname\b/.test(first) && (first.includes('city') || first.includes('note')) ? 1 : 0;

    const out: GiftRecipient[] = [];
    for (let i = start; i < lines.length; i++) {
      const cols = lines[i].split(',').map(c => c.trim());
      const name = cols[0] ?? '';
      if (!name) continue; // a row must at least name someone
      out.push({ name, city: cols[1] ?? '', notes: cols.slice(2).join(', ') });
    }
    return out;
  }

  protected removeRow(index: number): void {
    this.recipients.update(r => r.filter((_, i) => i !== index));
  }

  protected canGenerate(): boolean {
    return !this.streaming() && this.recipients().some(r => r.name.trim().length > 0);
  }

  protected async generate(): Promise<void> {
    if (!this.canGenerate()) return;
    this.error.set(null);
    this.draft.set(null);
    this.assistantText.set('');
    this.activity.set(null);
    this.streaming.set(true);

    const prompt = this.buildPrompt();
    try {
      await this.agent.streamChat([{ role: 'user', content: prompt }], {
        onMessage: t => this.assistantText.update(s => s + t),
        onTool: name => this.activity.set(TOOL_LABELS[name] ?? 'Working…'),
        onDraft: d => this.draft.set(d),
        onError: msg => this.error.set(msg),
        onDone: () => this.activity.set(null)
      });
    } finally {
      this.streaming.set(false);
      this.activity.set(null);
    }
  }

  private buildPrompt(): string {
    const rows = this.recipients().filter(r => r.name.trim());
    const occasion = this.occasion.trim() || 'a corporate gifting occasion';
    const budget = this.budgetRupees
      ? `, budget about ₹${this.budgetRupees} per recipient`
      : '';
    const lines = rows.map((r, i) => {
      const parts = [r.name, r.city, r.notes].map(p => p.trim()).filter(Boolean);
      return `${i + 1}. ${parts.join(' — ')}`;
    });
    return `I need corporate gifts for ${rows.length} recipients for ${occasion}${budget}.\n`
      + `Recipients:\n${lines.join('\n')}\n\n`
      + `Propose a consolidated selection (a small set of products with quantities that cover all `
      + `recipients), explain briefly why each fits, respect any dietary or cultural notes, and give `
      + `the estimated total. Then create a draft cart for the selection.`;
  }

  protected adopt(): void {
    void this.agent.adoptDraft(this.draft());
    this.draft.set(null);
  }

  protected exportPo(): void {
    const d = this.draft();
    if (!d) return;
    const rows = this.recipients().filter(r => r.name.trim());
    const esc = (v: string | number) => {
      const s = String(v);
      return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
    };

    const out: string[] = [];
    out.push(`Purchase Order — Gift Plan`);
    out.push(`Occasion,${esc(this.occasion.trim() || 'N/A')}`);
    out.push(`Recipients,${rows.length}`);
    out.push('');
    out.push('Product,Quantity,Unit Price (INR),Line Total (INR)');
    for (const line of d.lines) {
      out.push([
        esc(line.productName),
        line.quantity,
        (line.unitPriceCents / 100).toFixed(2),
        (line.lineTotalCents / 100).toFixed(2)
      ].join(','));
    }
    out.push('');
    out.push(`Grand Total (INR),${(d.totalCents / 100).toFixed(2)}`);

    const blob = new Blob([out.join('\n')], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'gift-plan-PO.csv';
    a.click();
    URL.revokeObjectURL(url);
  }
}
