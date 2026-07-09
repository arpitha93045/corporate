import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { Category, Product } from '../../models/models';
import { MoneyPipe } from '../../shared/money.pipe';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [RouterLink, FormsModule, MoneyPipe],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.css'
})
export class CatalogComponent {
  private api = inject(ApiService);

  protected categories = signal<Category[]>([]);
  protected products = signal<Product[]>([]);
  protected loading = signal<boolean>(true);
  protected error = signal<string | null>(null);
  protected selectedCategory = signal<string | null>(null);

  protected search = signal<string>('');
  protected minPrice = signal<number | null>(null);
  protected maxPrice = signal<number | null>(null);
  protected sortBy = signal<'default' | 'price-asc' | 'price-desc' | 'name'>('default');

  protected visibleProducts = computed<Product[]>(() => {
    const q = this.search().trim().toLowerCase();
    const min = this.minPrice();
    const max = this.maxPrice();
    const filtered = this.products().filter(p => {
      const rupees = p.priceCents / 100;
      if (min != null && rupees < min) return false;
      if (max != null && rupees > max) return false;
      if (q && !(p.name.toLowerCase().includes(q) || p.description.toLowerCase().includes(q))) return false;
      return true;
    });
    const sorted = [...filtered];
    switch (this.sortBy()) {
      case 'price-asc':  sorted.sort((a, b) => a.priceCents - b.priceCents); break;
      case 'price-desc': sorted.sort((a, b) => b.priceCents - a.priceCents); break;
      case 'name':       sorted.sort((a, b) => a.name.localeCompare(b.name)); break;
    }
    return sorted;
  });

  protected hasActiveFilters = computed(() =>
    this.search().trim().length > 0 || this.minPrice() != null || this.maxPrice() != null
  );

  constructor() {
    this.api.categories().subscribe({
      next: c => this.categories.set(c),
      error: () => {}
    });
    this.loadProducts(null);
  }

  protected selectCategory(slug: string | null): void {
    this.selectedCategory.set(slug);
    this.loadProducts(slug);
  }

  protected clearFilters(): void {
    this.search.set('');
    this.minPrice.set(null);
    this.maxPrice.set(null);
  }

  private loadProducts(slug: string | null): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.products(slug ?? undefined).subscribe({
      next: p => { this.products.set(p); this.loading.set(false); },
      error: () => { this.error.set('Could not load products. Is the backend running?'); this.loading.set(false); }
    });
  }
}
