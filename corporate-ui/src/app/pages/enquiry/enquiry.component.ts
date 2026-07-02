import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { EnquiryRequest } from '../../models/models';

@Component({
  selector: 'app-enquiry',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './enquiry.component.html',
  styleUrl: './enquiry.component.css'
})
export class EnquiryComponent {
  private fb = inject(FormBuilder);
  private api = inject(ApiService);

  protected submitting = signal<boolean>(false);
  protected errorMsg = signal<string | null>(null);
  protected submitted = signal<boolean>(false);

  protected form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    companyName: [''],
    phone: [''],
    estimatedQuantity: [null as number | null, [Validators.min(1)]],
    occasion: [''],
    eventDate: [''],
    budgetRange: [''],
    message: ['', [Validators.required, Validators.maxLength(5000)]]
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMsg.set(null);
    const v = this.form.getRawValue();
    const payload: EnquiryRequest = {
      name: v.name,
      email: v.email,
      companyName: v.companyName || undefined,
      phone: v.phone || undefined,
      estimatedQuantity: v.estimatedQuantity ?? undefined,
      occasion: v.occasion || undefined,
      eventDate: v.eventDate || undefined,
      budgetRange: v.budgetRange || undefined,
      message: v.message
    };
    this.api.submitEnquiry(payload).subscribe({
      next: () => {
        this.submitting.set(false);
        this.submitted.set(true);
        this.form.reset();
      },
      error: err => {
        this.submitting.set(false);
        this.errorMsg.set(err?.error?.message ?? 'Could not submit your enquiry. Please try again.');
      }
    });
  }
}
