import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { Order } from '../../models/models';
import { MoneyPipe } from '../../shared/money.pipe';

@Component({
  selector: 'app-order-confirmation',
  standalone: true,
  imports: [RouterLink, MoneyPipe],
  templateUrl: './order-confirmation.component.html',
  styleUrl: './order-confirmation.component.css'
})
export class OrderConfirmationComponent {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);

  protected order = signal<Order | null>(null);
  protected loading = signal<boolean>(true);
  protected notFound = signal<boolean>(false);

  constructor() {
    const orderNumber = this.route.snapshot.paramMap.get('orderNumber');
    if (!orderNumber) {
      this.notFound.set(true);
      this.loading.set(false);
      return;
    }
    this.api.order(orderNumber).subscribe({
      next: o => { this.order.set(o); this.loading.set(false); },
      error: () => { this.notFound.set(true); this.loading.set(false); }
    });
  }
}
