import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import type { AppNotification, Bet } from '../data/winup-data';
import { toBet, toMovement, toNotification } from './winup-api.mappers';
import type {
  BetResponse,
  DepositResponse,
  MatchPage,
  MatchQuery,
  NotificationResponse,
  SportSummary,
  WalletResponse,
  WithdrawalResponse,
} from './winup-api.types';

export * from './winup-api.types';
export { toBet, toMovement, toNotification } from './winup-api.mappers';

@Injectable({ providedIn: 'root' })
export class WinupApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1';

  matches(query: MatchQuery): Observable<MatchPage> {
    const params = new HttpParams()
      .set('status', query.status)
      .set('sport', query.sport)
      .set('page', query.page)
      .set('size', query.size);
    return this.http.get<MatchPage>(`${this.base}/matches`, { params });
  }

  bets(): Observable<Bet[]> {
    return this.http
      .get<BetResponse[]>(`${this.base}/bets`)
      .pipe(map((items) => items.map(toBet)));
  }

  sports(): Observable<SportSummary[]> {
    return this.http.get<SportSummary[]>(`${this.base}/sports`);
  }

  wallet(): Observable<WalletResponse> {
    return this.http.get<WalletResponse>(`${this.base}/wallet`);
  }

  notifications(): Observable<AppNotification[]> {
    return this.http
      .get<NotificationResponse[]>(`${this.base}/notifications`)
      .pipe(map((items) => items.map(toNotification)));
  }

  readNotifications(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/notifications/read`);
  }

  markNotificationsRead(ids: string[]): Observable<void> {
    return this.http.post<void>(`${this.base}/notifications/read`, { ids });
  }

  placeBet(matchId: string, selection: string, stake: number): Observable<Bet> {
    return this.http
      .post<BetResponse>(
        `${this.base}/bets`,
        {
          matchId,
          selection,
          stake: stake.toFixed(2),
        },
        { headers: { 'Idempotency-Key': this.idempotencyKey() } },
      )
      .pipe(map(toBet));
  }

  createDeposit(amount: number, returnUrl?: string): Observable<DepositResponse> {
    return this.http.post<DepositResponse>(
      `${this.base}/payments/deposits`,
      { amount: amount.toFixed(2), returnUrl: returnUrl ?? null },
      { headers: { 'Idempotency-Key': this.idempotencyKey() } },
    );
  }

  createWithdrawal(amount: number): Observable<WithdrawalResponse> {
    return this.http.post<WithdrawalResponse>(
      `${this.base}/payments/withdrawals`,
      { amount: amount.toFixed(2) },
      { headers: { 'Idempotency-Key': this.idempotencyKey() } },
    );
  }

  withdrawals(): Observable<WithdrawalResponse[]> {
    return this.http.get<WithdrawalResponse[]>(`${this.base}/payments/withdrawals`);
  }

  simulateDeposit(id: string): Observable<DepositResponse> {
    return this.http.post<DepositResponse>(
      `${this.base}/dev/payments/${id}/simulate`,
      {},
      { params: new HttpParams().set('outcome', 'APPROVED') },
    );
  }

  private idempotencyKey(): string {
    if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
      return crypto.randomUUID();
    }
    return `${Date.now()}-${Math.random()}`;
  }
}
