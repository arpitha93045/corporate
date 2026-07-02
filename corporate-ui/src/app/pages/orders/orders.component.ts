import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { OrderSummary } from '../../models/models';
import { MoneyPipe } from '../../shared/money.pipe';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [RouterLink, DatePipe, MoneyPipe],
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.css'
})
export class OrdersComponent {
  private api = inject(ApiService);

  protected orders = signal<OrderSummary[]>([]);
  protected loading = signal<boolean>(true);
  protected error = signal<string | null>(null);

  constructor() {
    this.api.myOrders().subscribe({
      next: o => { this.orders.set(o); this.loading.set(false); },
      error: () => { this.error.set('Could not load your orders.'); this.loading.set(false); }
    });
  }
}
