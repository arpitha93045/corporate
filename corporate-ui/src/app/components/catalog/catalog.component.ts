import { Component, computed, effect, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { Category, Product } from '../../models/models';
import { MoneyPipe } from '../../shared/money.pipe';
import { AnimateOnScrollDirective } from '../../shared/directives/animate-on-scroll.directive';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [RouterLink, FormsModule, MoneyPipe, AnimateOnScrollDirective],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.css'
})
export class CatalogComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);

  protected categories = signal<Category[]>([]);
  protected products = signal<Product[]>([]);
  protected loading = signal<boolean>(true);
  protected error = signal<string | null>(null);
  protected selectedCategory = signal<string | null>(null);

  protected search = signal<string>('');
  protected minPrice = signal<number | null>(null);
  protected maxPrice = signal<number | null>(null);
  protected sortBy = signal<'default' | 'price-asc' | 'price-desc' | 'name'>('default');

  private searchTimer: ReturnType<typeof setTimeout> | null = null;
  private lastQuerySent = '';

  // Price + sort refine the server-returned set client-side. Text search and
  // category filtering are done server-side so results aren't limited to the
  // products already loaded in the browser.
  protected visibleProducts = computed<Product[]>(() => {
    const min = this.minPrice();
    const max = this.maxPrice();
    const filtered = this.products().filter(p => {
      const rupees = p.priceCents / 100;
      if (min != null && rupees < min) return false;
      if (max != null && rupees > max) return false;
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

    // Debounce free-text search so we don't fire a request per keystroke.
    effect(() => {
      const q = this.search().trim();
      if (this.searchTimer) clearTimeout(this.searchTimer);
      this.searchTimer = setTimeout(() => {
        if (q === this.lastQuerySent) return;
        this.lastQuerySent = q;
        this.loadProducts();
      }, 300);
    });
  }

  ngOnInit(): void {
    // Subscribe to query params for category filtering from URL
    this.route.queryParams.subscribe(params => {
      const category = params['category'] || null;
      this.selectedCategory.set(category);
      this.loadProducts();
    });
  }

  protected selectCategory(slug: string | null): void {
    this.selectedCategory.set(slug);
    this.loadProducts();
  }

  protected clearFilters(): void {
    this.search.set('');
    this.minPrice.set(null);
    this.maxPrice.set(null);
  }

  protected getCategoryIcon(slug: string): string {
    const icons: Record<string, string> = {
      'welcome-kits': '👋',
      'hampers': '🧺',
      'drinkware': '☕',
      'tech': '💻',
      'plants': '🌱',
      'chocolates': '🍫',
      'stationery': '✏️',
      'bags': '👜',
      'wellness': '🧘',
      'home': '🏠',
      'accessories': '⌚',
      'apparel': '👕'
    };
    return icons[slug] || '🎁';
  }

  private loadProducts(): void {
    this.loading.set(true);
    this.error.set(null);
    const slug = this.selectedCategory();
    const q = this.search().trim();
    this.lastQuerySent = q;
    this.api.products(slug ?? undefined, q || undefined).subscribe({
      next: p => { this.products.set(p); this.loading.set(false); },
      error: () => { this.error.set('Could not load products. Is the backend running?'); this.loading.set(false); }
    });
  }
}
