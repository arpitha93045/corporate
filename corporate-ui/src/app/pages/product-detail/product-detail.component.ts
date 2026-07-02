import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { CartService } from '../../core/cart.service';
import { Product } from '../../models/models';
import { MoneyPipe } from '../../shared/money.pipe';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [RouterLink, MoneyPipe],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.css'
})
export class ProductDetailComponent {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  protected cart = inject(CartService);

  protected product = signal<Product | null>(null);
  protected quantity = signal<number>(1);
  protected loading = signal<boolean>(true);
  protected notFound = signal<boolean>(false);

  constructor() {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (!slug) {
      this.notFound.set(true);
      this.loading.set(false);
      return;
    }
    this.api.product(slug).subscribe({
      next: p => { this.product.set(p); this.loading.set(false); },
      error: () => { this.notFound.set(true); this.loading.set(false); }
    });
  }

  protected changeQty(delta: number): void {
    this.quantity.update(q => Math.max(1, q + delta));
  }

  protected addToCart(): void {
    const p = this.product();
    if (!p) return;
    this.cart.add(p, this.quantity());
    this.router.navigate(['/cart']);
  }
}
