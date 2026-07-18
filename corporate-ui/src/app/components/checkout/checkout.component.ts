import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { CartService } from '../../core/cart.service';
import { MoneyPipe } from '../../shared/money.pipe';
import { PaymentTerms } from '../../models/models';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MoneyPipe],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.css'
})
export class CheckoutComponent {
  private fb = inject(FormBuilder);
  private api = inject(ApiService);
  private router = inject(Router);
  private auth = inject(AuthService);
  protected cart = inject(CartService);

  protected submitting = signal<boolean>(false);
  protected errorMsg = signal<string | null>(null);

  private idempotencyKey: string | null = null;

  protected form = this.fb.nonNullable.group({
    companyName: [this.auth.user()?.companyName ?? '', Validators.required],
    contactName: [this.auth.user()?.fullName ?? '', Validators.required],
    email: [this.auth.user()?.email ?? '', [Validators.required, Validators.email]],
    phone: [this.auth.user()?.phone ?? ''],
    line1: ['', Validators.required],
    line2: [''],
    city: ['', Validators.required],
    state: [''],
    postalCode: ['', Validators.required],
    country: ['', Validators.required],
    paymentTerms: ['IMMEDIATE' as PaymentTerms],
    poNumber: ['']
  });

  constructor() {
    // A PO number is required only when paying by invoice (net-30). Toggle the
    // validator with the choice and clear a stale PO when switching back to card.
    this.form.controls.paymentTerms.valueChanges.subscribe(terms => {
      const po = this.form.controls.poNumber;
      if (terms === 'NET_30') {
        po.addValidators(Validators.required);
      } else {
        po.clearValidators();
        po.setValue('');
      }
      po.updateValueAndValidity();
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.cart.lines().length === 0) {
      this.errorMsg.set('Your cart is empty.');
      return;
    }

    const v = this.form.getRawValue();
    this.submitting.set(true);
    this.errorMsg.set(null);

    // Reuse the same key across retries of the same checkout attempt so a
    // network blip / user-hit-again doesn't create a duplicate order. The
    // backend keys idempotency on (user_id, key), so a per-attempt UUID is
    // enough. Cleared on success and on error paths that mean "start over".
    if (!this.idempotencyKey) {
      this.idempotencyKey = crypto.randomUUID();
    }

    this.api.checkout({
      customer: {
        companyName: v.companyName,
        contactName: v.contactName,
        email: v.email,
        phone: v.phone
      },
      shippingAddress: {
        line1: v.line1, line2: v.line2 || null,
        city: v.city, state: v.state || null,
        postalCode: v.postalCode, country: v.country
      },
      items: this.cart.lines().map(l => ({
        productId: l.product.id,
        quantity: l.quantity,
        branding: l.branding ?? null
      })),
      paymentTerms: v.paymentTerms,
      poNumber: v.paymentTerms === 'NET_30' ? v.poNumber : undefined
    }, this.idempotencyKey).subscribe({
      next: order => {
        this.idempotencyKey = null;
        this.cart.clear();
        // Card orders go to Stripe; net-30 invoice orders are already confirmed
        // (payable later) so go straight to the confirmation page.
        if (order.paymentTerms === 'NET_30') {
          this.router.navigate(['/order', order.orderNumber]);
        } else {
          this.router.navigate(['/pay', order.orderNumber]);
        }
      },
      error: err => {
        this.submitting.set(false);
        // 4xx means the request itself is bad (validation, out of stock,
        // unauthorized) — the user needs to change something, so a retry
        // should be a fresh attempt with a new key. 5xx / network errors are
        // transient; keep the key so retrying replays safely.
        const status = err?.status ?? 0;
        if (status >= 400 && status < 500) {
          this.idempotencyKey = null;
        }
        this.errorMsg.set(err?.error?.message ?? 'Could not place order. Please try again.');
      }
    });
  }
}
