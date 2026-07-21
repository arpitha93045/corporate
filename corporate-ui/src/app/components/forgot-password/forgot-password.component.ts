import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: '../login/login.component.css'
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);

  protected submitting = signal<boolean>(false);
  protected sent = signal<boolean>(false);

  protected form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    // Always show the same confirmation regardless of whether the email exists —
    // never reveal which addresses are registered.
    this.auth.forgotPassword(this.form.getRawValue()).subscribe({
      next: () => { this.submitting.set(false); this.sent.set(true); },
      error: () => { this.submitting.set(false); this.sent.set(true); }
    });
  }
}
