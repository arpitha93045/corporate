import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { loadStripe, Stripe, StripeElements, StripePaymentElement } from '@stripe/stripe-js';
import { ApiService } from '../../core/api.service';
import { APP_CONFIG } from '../../core/config';
import { Order } from '../../models/models';
import { MoneyPipe } from '../../shared/money.pipe';

@Component({
  selector: 'app-pay',
  standalone: true,
  imports: [RouterLink, MoneyPipe],
  templateUrl: './pay.component.html',
  styleUrl: './pay.component.css',
})
export class PayComponent {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  protected order = signal<Order | null>(null);
  protected loading = signal<boolean>(true);
  protected submitting = signal<boolean>(false);
  protected error = signal<string | null>(null);

  @ViewChild('paymentElement', { static: false }) paymentEl!: ElementRef<HTMLDivElement>;

  private stripe: Stripe | null = null;
  private elements: StripeElements | null = null;
  private paymentElement: StripePaymentElement | null = null;

  async ngOnInit() {
    const orderNumber = this.route.snapshot.paramMap.get('orderNumber');
    if (!orderNumber) {
      this.error.set('Missing order number');
      this.loading.set(false);
      return;
    }
    try {
      const order = await this.api.order(orderNumber).toPromise();
      this.order.set(order!);
      if (order!.status === 'PAID') {
        this.router.navigate(['/order', orderNumber]);
        return;
      }
      const intent = await this.api.createPaymentIntent(orderNumber).toPromise();
      this.stripe = await loadStripe(APP_CONFIG.stripePublishableKey);
      if (!this.stripe) throw new Error('Stripe.js failed to load');
      this.elements = this.stripe.elements({ clientSecret: intent!.clientSecret });
      this.paymentElement = this.elements.create('payment');
      this.loading.set(false);
      // Wait a tick so @if renders the mount div before we attach.
      setTimeout(() => this.paymentElement?.mount(this.paymentEl.nativeElement), 0);
    } catch (e: any) {
      this.error.set(e?.error?.message ?? e?.message ?? 'Failed to initialize payment');
      this.loading.set(false);
    }
  }

  async pay() {
    if (!this.stripe || !this.elements || this.submitting()) return;
    this.submitting.set(true);
    this.error.set(null);
    const orderNumber = this.order()!.orderNumber;
    const { error, paymentIntent } = await this.stripe.confirmPayment({
      elements: this.elements,
      confirmParams: {
        return_url: `${window.location.origin}/order/${orderNumber}`,
      },
      redirect: 'if_required',
    });
    if (error) {
      this.error.set(error.message ?? 'Payment failed');
      this.submitting.set(false);
      return;
    }
    if (paymentIntent && paymentIntent.status === 'succeeded') {
      // Webhook flips the order to PAID; navigate to confirmation which will
      // reflect the new status (may briefly show PLACED if the webhook is slow).
      this.router.navigate(['/order', orderNumber]);
    } else {
      this.submitting.set(false);
      this.error.set('Payment did not complete');
    }
  }
}
