import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, UserSummary } from '../models/models';

const TOKEN_KEY = 'cg.auth.token';
const USER_KEY = 'cg.auth.user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private base = '/api/auth';

  private tokenSig = signal<string | null>(this.readToken());
  private userSig = signal<UserSummary | null>(this.readUser());

  readonly user = this.userSig.asReadonly();
  readonly token = this.tokenSig.asReadonly();
  readonly isAuthenticated = computed(() => this.tokenSig() !== null);

  register(req: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/register`, req).pipe(
      tap(res => this.persist(res))
    );
  }

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/login`, req).pipe(
      tap(res => this.persist(res))
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.tokenSig.set(null);
    this.userSig.set(null);
  }

  refreshMe(): Observable<UserSummary> {
    return this.http.get<UserSummary>(`${this.base}/me`).pipe(
      tap(u => {
        localStorage.setItem(USER_KEY, JSON.stringify(u));
        this.userSig.set(u);
      })
    );
  }

  private persist(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    this.tokenSig.set(res.token);
    this.userSig.set(res.user);
  }

  private readToken(): string | null {
    try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
  }

  private readUser(): UserSummary | null {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) as UserSummary : null;
    } catch { return null; }
  }
}
