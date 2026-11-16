import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { Match } from '../data/winup-data';

export interface AdminDashboard {
  users: number;
  matches: number;
  bets: number;
  payments: number;
  ledgerDebits: string;
  ledgerCredits: string;
}

export interface AdminUser {
  id: string;
  email: string;
  displayName: string;
  role: string;
  status: string;
  createdAt: string;
}

export interface AdminPayment {
  id: string;
  userId: string;
  status: string;
  amountCop: string;
  provider: string;
  createdAt: string;
}

export interface AdminLedger {
  id: string;
  transactionId: string;
  account: string;
  debit: string;
  credit: string;
  currency: string;
  concept: string;
  createdAt: string;
}

export interface CreateMatchPayload {
  sport: string;
  league: string;
  home: string;
  away: string;
  localOdds: number;
  drawOdds: number | null;
  awayOdds: number;
  minute: number;
}

export interface AdminMatchPage {
  items: Match[];
  page: number;
  size: number;
  total: number;
  hasMore: boolean;
}

export interface AdminPromo {
  id: string;
  title: string;
  detail: string;
  status: string;
  scheduledAt: string;
  createdAt: string;
  sentAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/admin';

  dashboard(): Observable<AdminDashboard> {
    return this.http.get<AdminDashboard>(`${this.base}/dashboard`);
  }

  users(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.base}/users`);
  }

  payments(): Observable<AdminPayment[]> {
    return this.http.get<AdminPayment[]>(`${this.base}/payments`);
  }

  ledger(): Observable<AdminLedger[]> {
    return this.http.get<AdminLedger[]>(`${this.base}/ledger`);
  }

  matches(page: number, size: number, status: string, sport = 'TODOS'): Observable<AdminMatchPage> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('status', status)
      .set('sport', sport);
    return this.http.get<AdminMatchPage>(`${this.base}/matches/page`, { params });
  }

  updateRole(id: string, role: string): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.base}/users/${id}/role`, { role });
  }

  createMatch(payload: CreateMatchPayload): Observable<Match> {
    return this.http.post<Match>(`${this.base}/matches`, payload);
  }

  updateOdds(
    id: string,
    odds: { localOdds: number; drawOdds: number | null; awayOdds: number },
  ): Observable<Match> {
    return this.http.put<Match>(`${this.base}/matches/${id}/odds`, odds);
  }

  promotions(): Observable<AdminPromo[]> {
    return this.http.get<AdminPromo[]>(`${this.base}/promotions`);
  }

  createPromotion(title: string, detail: string, scheduledAt: string | null): Observable<AdminPromo> {
    return this.http.post<AdminPromo>(`${this.base}/promotions`, { title, detail, scheduledAt });
  }

  cancelPromotion(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/promotions/${id}/cancel`, {});
  }

  ensureSports(): Observable<{ activeSports: number }> {
    return this.http.post<{ activeSports: number }>(`${this.base}/matches/ensure-sports`, {});
  }

  seed(count = 300): Observable<{ created: number }> {
    const params = new HttpParams().set('count', count);
    return this.http.post<{ created: number }>(`${this.base}/matches/seed`, {}, { params });
  }
}
