import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CartService } from './core/cart.service';
import { AuthService } from './core/auth.service';
import { AgentChatComponent } from './shared/agent-chat/agent-chat.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AgentChatComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected cart = inject(CartService);
  protected auth = inject(AuthService);
  private router = inject(Router);
  protected year = new Date().getFullYear();

  protected logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/');
  }
}
