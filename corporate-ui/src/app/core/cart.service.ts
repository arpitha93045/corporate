import { Injectable, computed, signal } from '@angular/core';
import { CartLine, Product } from '../models/models';

const STORAGE_KEY = 'corporate-gifting-cart-v1';

interface StoredLine {
  product: Product;
  quantity: number;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly _lines = signal<CartLine[]>(this.load());

  readonly lines = this._lines.asReadonly();
  readonly count = computed(() => this._lines().reduce((n, l) => n + l.quantity, 0));
  readonly subtotalCents = computed(() =>
    this._lines().reduce((sum, l) => sum + l.product.priceCents * l.quantity, 0)
  );

  add(product: Product, quantity = 1): void {
    const next = [...this._lines()];
    const existing = next.find(l => l.product.id === product.id);
    if (existing) {
      existing.quantity += quantity;
    } else {
      next.push({ product, quantity });
    }
    this.commit(next);
  }

  setQuantity(productId: number, quantity: number): void {
    if (quantity <= 0) {
      this.remove(productId);
      return;
    }
    const next = this._lines().map(l =>
      l.product.id === productId ? { ...l, quantity } : l
    );
    this.commit(next);
  }

  remove(productId: number): void {
    this.commit(this._lines().filter(l => l.product.id !== productId));
  }

  clear(): void {
    this.commit([]);
  }

  private commit(lines: CartLine[]): void {
    this._lines.set(lines);
    const stored: StoredLine[] = lines.map(l => ({ product: l.product, quantity: l.quantity }));
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
