import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartService } from './core/cart.service';
import { AuthService } from './core/auth.service';
import { ApiService } from './core/api.service';
import { AgentChatComponent } from './shared/agent-chat/agent-chat.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AgentChatComponent, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected cart = inject(CartService);
  protected auth = inject(AuthService);
  private api = inject(ApiService);
  private router = inject(Router);
  protected year = new Date().getFullYear();

  // Newsletter
  protected newsletterEmail = signal('');
  protected newsletterStatus = signal<'idle' | 'loading' | 'success' | 'error'>('idle');
  protected newsletterMessage = signal('');

  protected logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/');
  }

  protected subscribeNewsletter(): void {
    const email = this.newsletterEmail().trim();
    if (!email) return;

    this.newsletterStatus.set('loading');
    this.api.subscribeNewsletter(email).subscribe({
      next: (res) => {
        this.newsletterStatus.set('success');
        this.newsletterMessage.set(res.message);
        this.newsletterEmail.set('');
      },
      error: (err) => {
        this.newsletterStatus.set('error');
        this.newsletterMessage.set(err.error?.message || 'Failed to subscribe. Please try again.');
      }
    });
  }
}
