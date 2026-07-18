import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartService } from '../../core/cart.service';
import { Branding } from '../../models/models';
import { MoneyPipe } from '../../shared/money.pipe';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [RouterLink, FormsModule, MoneyPipe],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.css'
})
export class CartComponent {
  protected cart = inject(CartService);

  protected change(index: number, raw: string | number): void {
    const n = typeof raw === 'string' ? parseInt(raw, 10) : raw;
    if (Number.isFinite(n)) this.cart.setQuantity(index, n);
  }

  protected remove(index: number): void {
    this.cart.remove(index);
  }

  protected setMessage(index: number, current: Branding | undefined, message: string): void {
    this.cart.setBranding(index, { message, logoUrl: current?.logoUrl ?? null });
  }

  protected setLogoUrl(index: number, current: Branding | undefined, logoUrl: string): void {
    this.cart.setBranding(index, { message: current?.message ?? null, logoUrl });
  }
}
