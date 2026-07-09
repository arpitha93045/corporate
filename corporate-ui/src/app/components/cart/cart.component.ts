import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartService } from '../../core/cart.service';
import { MoneyPipe } from '../../shared/money.pipe';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [RouterLink, MoneyPipe],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.css'
})
export class CartComponent {
  protected cart = inject(CartService);

  protected change(productId: number, raw: string | number): void {
    const n = typeof raw === 'string' ? parseInt(raw, 10) : raw;
    if (Number.isFinite(n)) this.cart.setQuantity(productId, n);
  }

  protected remove(productId: number): void {
    this.cart.remove(productId);
  }
}
