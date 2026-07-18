import { Injectable, computed, signal } from '@angular/core';
import { Branding, CartLine, Product } from '../models/models';

const STORAGE_KEY = 'corporate-gifting-cart-v2';

interface StoredLine {
  product: Product;
  quantity: number;
  branding?: Branding;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly _lines = signal<CartLine[]>(this.load());

  readonly lines = this._lines.asReadonly();
  readonly count = computed(() => this._lines().reduce((n, l) => n + l.quantity, 0));
  readonly subtotalCents = computed(() =>
    this._lines().reduce((sum, l) => sum + l.product.priceCents * l.quantity, 0)
  );

  add(product: Product, quantity = 1, branding?: Branding): void {
    const norm = normalizeBranding(branding);
    const next = [...this._lines()];
    // Merge only when both product and branding match, mirroring the server's
    // checkout grouping — differing branding stays as a separate line.
    const existing = next.find(l => l.product.id === product.id && brandingEquals(l.branding, norm));
    if (existing) {
      existing.quantity += quantity;
    } else {
      next.push({ product, quantity, branding: norm });
    }
    this.commit(next);
  }

  setQuantity(index: number, quantity: number): void {
    if (quantity <= 0) {
      this.remove(index);
      return;
    }
    const next = this._lines().map((l, i) => (i === index ? { ...l, quantity } : l));
    this.commit(next);
  }

  setBranding(index: number, branding: Branding | undefined): void {
    const norm = normalizeBranding(branding);
    const next = this._lines().map((l, i) => (i === index ? { ...l, branding: norm } : l));
    this.commit(next);
  }

  remove(index: number): void {
    this.commit(this._lines().filter((_, i) => i !== index));
  }

  clear(): void {
    this.commit([]);
  }

  private commit(lines: CartLine[]): void {
    this._lines.set(lines);
    const stored: StoredLine[] = lines.map(l => ({ product: l.product, quantity: l.quantity, branding: l.branding }));
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(stored));
    } catch {}
  }

  private load(): CartLine[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return [];
      const parsed = JSON.parse(raw) as StoredLine[];
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
}

function normalizeBranding(b: Branding | undefined | null): Branding | undefined {
  if (!b) return undefined;
  const message = b.message?.trim() || null;
  const logoUrl = b.logoUrl?.trim() || null;
  if (!message && !logoUrl) return undefined;
  return { message, logoUrl };
}

function brandingEquals(a: Branding | undefined, b: Branding | undefined): boolean {
  const am = a?.message ?? null, al = a?.logoUrl ?? null;
  const bm = b?.message ?? null, bl = b?.logoUrl ?? null;
  return am === bm && al === bl;
}
