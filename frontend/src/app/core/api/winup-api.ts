import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import type { AppNotification, Bet, Match, Movement } from '../data/winup-data';

export interface BetOutcomeResponse {
  winner: string | null;
  winnerName: string | null;
  homeScore: number | null;
  awayScore: number | null;
  finished: boolean;
}

export interface BetResponse {
  id: string;
  match: string;
  market: string;
  selection: string;
  stake: string;
  odds: number;
  potential: string;
  status: string;
  placedAt: string;
  outcome?: BetOutcomeResponse | null;
}

export interface MovementResponse {
  id: string;
  concept: string;
  date: string;
  amount: string;
  type: string;
}

export interface NotificationResponse {
  id: string;
  kind: string;
  title: string;
  detail: string;
  amount: string | null;
  date: string;
}

export interface WalletResponse {
  balance: string;
  heldBalance: string;
  currency: string;
  movements: MovementResponse[];
}

export interface WithdrawalResponse {
  id: string;
  userId: string;
  status: string;
  amountCop: string;
  provider: string;
  message: string | null;
  createdAt: string;
}

export interface DepositResponse {
  id: string;
  status: string;
  amountCop: string;
  checkoutUrl: string | null;
  provider: string;
}

const NOTIFICATION_ICONS: Record<string, string> = {
  GANADA: 'lucideTrendingUp',
  PERDIDA: 'lucideCircleX',
  PENDIENTE: 'lucideClock3',
  PROMOCION: 'lucideTicket',
};

export function toNotification(dto: NotificationResponse): AppNotification {
  return {
    id: dto.id,
    kind: dto.kind as AppNotification['kind'],
    title: dto.title,
    detail: dto.detail,
    date: dto.date,
    icon: NOTIFICATION_ICONS[dto.kind] ?? 'lucideBell',
  };
}

export function toBet(dto: BetResponse): Bet {
  return {
    id: dto.id,
    match: dto.match,
    market: dto.market,
    selection: dto.selection,
    stake: Number(dto.stake),
    odds: dto.odds,
    potential: Number(dto.potential),
    status: dto.status as Bet['status'],
    placedAt: dto.placedAt,
    outcome: dto.outcome
      ? {
          winner: dto.outcome.winner ?? null,
          winnerName: dto.outcome.winnerName ?? null,
          homeScore: dto.outcome.homeScore ?? null,
          awayScore: dto.outcome.awayScore ?? null,
          finished: dto.outcome.finished ?? false,
        }
      : null,
  };
}

export function toMovement(dto: MovementResponse): Movement {
  return {
    id: dto.id,
    concept: dto.concept,
    date: dto.date,
    amount: Number(dto.amount),
    type: dto.type as Movement['type'],
  };
}

export interface MatchPage {
  items: Match[];
  page: number;
  size: number;
  total: number;
  hasMore: boolean;
}

export interface MatchQuery {
  status: string;
  sport: string;
  page: number;
  size: number;
}

export interface SportSummary {
  name: string;
  live: number;
  upcoming: number;
  finished: number;
  total: number;
}

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
      .post<BetResponse>(`${this.base}/bets`, {
        matchId,
        selection,
        stake: stake.toFixed(2),
      })
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
