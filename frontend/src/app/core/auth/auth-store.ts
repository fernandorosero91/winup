import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

export interface AuthUser {
  id: string;
  email: string;
  displayName: string;
  role?: string;
}

export interface AuthResponse {
  token: string;
  user: AuthUser;
}

const TOKEN_KEY = 'winup.token';
const USER_KEY = 'winup.user';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/auth';

  readonly token = signal<string | null>(readStored(TOKEN_KEY));
  readonly user = signal<AuthUser | null>(readUser());
  readonly isAuthenticated = computed(() => this.token() !== null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');
  readonly initials = computed(() => initialsOf(this.user()?.displayName));

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.base}/login`, { email, password })
      .pipe(tap((response) => this.persist(response)));
  }

  register(displayName: string, email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.base}/register`, { displayName, email, password })
      .pipe(tap((response) => this.persist(response)));
  }

  refreshProfile(): void {
    if (!this.token()) {
      return;
    }
    this.http.get<AuthUser>('/api/v1/me').subscribe({
      next: (user) => {
        this.user.set(user);
        sessionStorage.setItem(USER_KEY, JSON.stringify(user));
      },
      error: () => undefined,
    });
  }

  clear(): void {
    this.token.set(null);
    this.user.set(null);
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
  }

  private persist(response: AuthResponse): void {
    this.token.set(response.token);
    this.user.set(response.user);
    sessionStorage.setItem(TOKEN_KEY, response.token);
    sessionStorage.setItem(USER_KEY, JSON.stringify(response.user));
  }
}

function readStored(key: string): string | null {
  try {
    return sessionStorage.getItem(key);
  } catch {
    return null;
  }
}

function readUser(): AuthUser | null {
  const raw = readStored(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

function initialsOf(name: string | undefined): string {
  if (!name) {
    return 'U';
  }
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
}
