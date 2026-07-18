import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../core/api.service';
import { CartService } from '../core/cart.service';
import { DraftCart } from '../models/models';
import { MoneyPipe } from '../shared/money.pipe';

const MAX_LINES = 200;

interface ParsedLine {
  productSlug: string;
  quantity: number;
}

/**
 * Bulk-order CSV upload. A corporate buyer pastes/uploads {slug, quantity} rows;
 * the server re-prices every line against the catalog (POST /api/bulk-order/estimate)
 * and returns a priced draft cart. The buyer reviews the priced lines + warnings,
 * then adopts them into the normal cart and checks out through the existing flow.
 *
 * Adoption is a local loop (resolve slug -> Product -> CartService.add) rather than
 * AgentService.adoptDraft, which would push chat messages into the concierge drawer.
 */
@Component({
  selector: 'app-bulk-order',
  standalone: true,
  imports: [FormsModule, MoneyPipe],
  templateUrl: './bulk-order.component.html',
  styleUrl: './bulk-order.component.css'
})
export class BulkOrderComponent {
  private api = inject(ApiService);
  private cart = inject(CartService);
  private router = inject(Router);

  protected csvText = '';
  protected parsed = signal<ParsedLine[]>([]);
  protected parseError = signal<string | null>(null);

  protected loading = signal(false);
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
    this.draft.set(null);
    this.error.set(null);
    const rows = this.parseCsv(this.csvText);
    if (rows.length === 0) {
      this.parsed.set([]);
      this.parseError.set('No valid rows found. Expected lines like: product-slug, quantity');
      return;
    }
    if (rows.length > MAX_LINES) {
      this.parseError.set(`Too many rows (${rows.length}). Limit is ${MAX_LINES}.`);
      this.parsed.set(rows.slice(0, MAX_LINES));
      return;
    }
    this.parsed.set(rows);
  }

  private parseCsv(text: string): ParsedLine[] {
    const lines = text.split(/\r?\n/).map(l => l.trim()).filter(l => l.length > 0);
    if (lines.length === 0) return [];

    // Tolerate an optional header row.
    const first = lines[0].toLowerCase();
    const isHeader = (first.includes('slug') || first.includes('sku')) && first.includes('quantity');
    const start = isHeader ? 1 : 0;

    const out: ParsedLine[] = [];
    for (let i = start; i < lines.length; i++) {
      const cols = lines[i].split(',').map(c => c.trim());
      const slug = cols[0] ?? '';
      if (!slug) continue;
      const qty = Math.floor(Number(cols[1]));
      if (!Number.isFinite(qty) || qty < 1) continue; // drop obviously bad rows
      out.push({ productSlug: slug, quantity: qty });
    }
    return out;
  }

  protected removeRow(index: number): void {
    this.parsed.update(r => r.filter((_, i) => i !== index));
  }

  protected canEstimate(): boolean {
    return !this.loading() && this.parsed().length > 0;
  }

  protected async estimate(): Promise<void> {
    if (!this.canEstimate()) return;
    this.error.set(null);
    this.draft.set(null);
    this.loading.set(true);
    try {
      const req = { lines: this.parsed().map(l => ({ productSlug: l.productSlug, quantity: l.quantity })) };
      const draft = await firstValueFrom(this.api.bulkOrderEstimate(req));
      this.draft.set(draft);
    } catch {
      this.error.set('Could not price this order. Please check your rows and try again.');
    } finally {
      this.loading.set(false);
    }
  }

  protected async addAllToCart(): Promise<void> {
    const d = this.draft();
    if (!d || d.lines.length === 0) return;

    // Best-effort adoption beacon so agent conversion metrics stay consistent.
    if (d.token) {
      void fetch(`/api/agent/draft-cart/${encodeURIComponent(d.token)}`).catch(() => {});
    }

    for (const line of d.lines) {
      try {
        const product = await firstValueFrom(this.api.product(line.productSlug));
        this.cart.add(product, line.quantity);
      } catch {
        // A line the server priced but that no longer resolves is skipped silently;
        // the server-side warnings already cover unavailable items.
      }
    }
    this.draft.set(null);
    void this.router.navigate(['/cart']);
  }

  protected downloadTemplate(): void {
    const rows = [
      'slug,quantity',
      'artisan-chocolate-box,25',
      'ceramic-mug-duo,50'
    ];
    const blob = new Blob([rows.join('\n')], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'bulk-order-template.csv';
    a.click();
    URL.revokeObjectURL(url);
  }
}
