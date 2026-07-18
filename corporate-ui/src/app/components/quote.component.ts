import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { Quote } from '../models/models';
import { MoneyPipe } from '../shared/money.pipe';

@Component({
  selector: 'app-quote',
  standalone: true,
  imports: [RouterLink, MoneyPipe],
  templateUrl: './quote.component.html',
  styleUrl: './quote.component.css'
})
export class QuoteComponent {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);

  protected quote = signal<Quote | null>(null);
  protected loading = signal<boolean>(true);
  protected notFound = signal<boolean>(false);
  protected working = signal<boolean>(false);
  protected errorMsg = signal<string>('');

  private token = '';

  constructor() {
    const token = this.route.snapshot.paramMap.get('token');
    if (!token) {
      this.notFound.set(true);
      this.loading.set(false);
      return;
    }
    this.token = token;
    this.api.quote(token).subscribe({
      next: q => { this.quote.set(q); this.loading.set(false); },
      error: () => { this.notFound.set(true); this.loading.set(false); }
    });
  }

  protected get canRespond(): boolean {
    return this.quote()?.status === 'SENT';
  }

  protected accept(): void {
    this.respond(true);
  }

  protected decline(): void {
    this.respond(false);
  }

  private respond(accept: boolean): void {
    if (this.working()) return;
    this.working.set(true);
    this.errorMsg.set('');
    const call = accept ? this.api.acceptQuote(this.token) : this.api.declineQuote(this.token);
    call.subscribe({
      next: q => { this.quote.set(q); this.working.set(false); },
      error: err => {
        this.working.set(false);
        this.errorMsg.set(err?.status === 409
          ? 'This quote can no longer be changed.'
          : 'Something went wrong — please try again.');
      }
    });
  }
}
