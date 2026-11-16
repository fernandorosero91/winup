import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap, throwError } from 'rxjs';
import { WinupApi, toMovement } from '../api/winup-api';
import type { RealtimeMatchEvent } from '../realtime/realtime.service';

export type MatchStatus = 'EN VIVO' | 'PRÓXIMO' | 'FINALIZADO' | 'SUSPENDIDO';
export type StatusFilter = 'TODOS' | 'EN VIVO' | 'PRÓXIMO' | 'FINALIZADO';
export type Selection = 'LOCAL' | 'EMPATE' | 'VISITANTE';
export type BetStatus = 'PENDIENTE' | 'GANADA' | 'PERDIDA';
export type NotificationKind = 'GANADA' | 'PERDIDA' | 'PENDIENTE' | 'PROMOCION';

export interface Match {
  id: string;
  sport: string;
  league: string;
  home: string;
  away: string;
  homeShort: string;
  awayShort: string;
  homeScore: number;
  awayScore: number;
  minute: number;
  status: MatchStatus;
  startsAt: string;
  odds: {
    local: number;
    draw: number;
    away: number;
  };
  drawMarket: boolean;
  source?: string;
  isSimulated?: boolean;
}

export interface BetOutcome {
  winner: string | null;
  winnerName: string | null;
  homeScore: number | null;
  awayScore: number | null;
  finished: boolean;
}

export interface Bet {
  id: string;
  matchId?: string;
  match: string;
  market: string;
  selection: string;
  stake: number;
  odds: number;
  potential: number;
  status: BetStatus;
  placedAt: string;
  outcome: BetOutcome | null;
}

export interface Movement {
  id: string;
  concept: string;
  date: string;
  amount: number;
  type: 'INGRESO' | 'GASTO' | 'GANANCIA' | 'EGRESO';
}

export interface Wallet {
  balance: number;
  heldBalance: number;
  currency: string;
}

export interface AppNotification {
  id: string;
  kind: NotificationKind;
  title: string;
  detail: string;
  date: string;
  icon: string;
}

type FetchMode = 'initial' | 'page' | 'background';

const PAGE_SIZE = 24;

@Injectable({ providedIn: 'root' })
export class WinupData {
  private readonly api = inject(WinupApi);
  private requestId = 0;

  refresh(): void {
    this.fetch(this.page(), PAGE_SIZE, 'background');
    this.api.bets().subscribe({
      next: (bets) => this.bets.set(bets),
      error: () => undefined,
    });
    this.api.notifications().subscribe({
      next: (notifications) => this.serverNotifications.set(notifications),
      error: () => undefined,
    });
    this.api.readNotifications().subscribe({
      next: (ids) => this.readNotifications.set(ids),
      error: () => undefined,
    });
    this.api.wallet().subscribe({
      next: (wallet) => {
        this.wallet.set({
          balance: Number(wallet.balance),
          heldBalance: Number(wallet.heldBalance ?? 0),
          currency: wallet.currency,
        });
        this.movements.set(wallet.movements.map(toMovement));
      },
      error: () => undefined,
    });
  }

  readonly statusFilter = signal<StatusFilter>('TODOS');
  readonly sportFilter = signal<string>('TODOS');
  readonly total = signal(0);
  readonly page = signal(0);
  readonly loading = signal(false);
  readonly loadingPage = signal(false);

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / PAGE_SIZE)));
  readonly rangeStart = computed(() =>
    this.total() === 0 ? 0 : this.page() * PAGE_SIZE + 1,
  );
  readonly rangeEnd = computed(() =>
    Math.min(this.total(), (this.page() + 1) * PAGE_SIZE),
  );

  setStatus(status: StatusFilter): void {
    if (this.statusFilter() === status) {
      return;
    }
    this.applyFilters(status, this.sportFilter());
  }

  setSport(sport: string): void {
    if (this.sportFilter() === sport) {
      return;
    }
    this.applyFilters(this.statusFilter(), sport);
  }

  applyFilters(status: StatusFilter, sport: string): void {
    this.statusFilter.set(status);
    this.sportFilter.set(sport);
    this.page.set(0);
    this.fetch(0, PAGE_SIZE, 'initial');
  }

  resetFilters(status: StatusFilter = 'TODOS'): void {
    this.applyFilters(status, 'TODOS');
  }

  goToPage(page: number): void {
    const target = Math.max(0, Math.min(this.totalPages() - 1, page));
    if (target === this.page()) {
      return;
    }
    this.page.set(target);
    this.fetch(target, PAGE_SIZE, 'page');
  }

  nextPage(): void {
    this.goToPage(this.page() + 1);
  }

  previousPage(): void {
    this.goToPage(this.page() - 1);
  }

  applyRealtime(event: RealtimeMatchEvent): void {
    this.matches.update((matches) =>
      matches.map((match) =>
        match.id === event.matchId
          ? {
              ...match,
              status: event.status === 'CLOSED' ? 'FINALIZADO' : match.status,
              homeScore: event.homeScore,
              awayScore: event.awayScore,
              minute: event.minute,
            }
          : match,
      ),
    );
  }

  private fetch(page: number, size: number, mode: FetchMode): void {
    const request = ++this.requestId;
    if (mode === 'initial') {
      this.loading.set(true);
    }
    if (mode === 'page') {
      this.loadingPage.set(true);
    }
    this.api
      .matches({
        status: this.statusFilter(),
        sport: this.sportFilter(),
        page,
        size,
      })
      .subscribe({
        next: (result) => {
          if (request !== this.requestId) {
            return;
          }
          this.matches.set(result.items);
          this.total.set(result.total);
          this.syncSelection();
          this.loading.set(false);
          this.loadingPage.set(false);
        },
        error: () => {
          if (request !== this.requestId) {
            return;
          }
          this.loading.set(false);
          this.loadingPage.set(false);
        },
      });
  }

  private syncSelection(): void {
    const current = this.selectedMatch();
    const matches = this.matches();
    if (!current || !matches.some((match) => match.id === current.id)) {
      this.selectedMatch.set(matches[0] ?? null);
      this.selectedSelection.set('LOCAL');
    }
  }

  placeBet(): Observable<Bet> {
    const match = this.selectedMatch();
    if (!match) {
      return throwError(() => new Error('Sin partido seleccionado'));
    }
    return this.api
      .placeBet(match.id, this.selectedSelection(), this.stake())
      .pipe(tap(() => this.refresh()));
  }

  readonly sports = [
    { name: 'Fútbol', label: 'Fútbol', icon: 'matSportsSoccerOutline' },
    { name: 'AFL', label: 'AFL', icon: 'matSportsOutline' },
    { name: 'Béisbol', label: 'Béisbol', icon: 'matSportsBaseballOutline' },
    { name: 'Baloncesto', label: 'Baloncesto', icon: 'matSportsBasketballOutline' },
    { name: 'Fórmula 1', label: 'Fórmula 1', icon: 'matSportsMotorsportsOutline' },
    { name: 'Balonmano', label: 'Balonmano', icon: 'matSportsHandballOutline' },
    { name: 'Hockey', label: 'Hockey', icon: 'matSportsHockeyOutline' },
    { name: 'MMA', label: 'MMA', icon: 'matSportsMmaOutline' },
    { name: 'NBA', label: 'NBA', icon: 'matSportsBasketballOutline' },
    { name: 'NFL & NCAA', label: 'NFL & NCAA', icon: 'matSportsFootballOutline' },
    { name: 'Rugby', label: 'Rugby', icon: 'matSportsRugbyOutline' },
    { name: 'Voleibol', label: 'Voleibol', icon: 'matSportsVolleyballOutline' },
    { name: 'Tenis', label: 'Tenis', icon: 'matSportsTennisOutline' },
    { name: 'eSports', label: 'eSports', icon: 'matSportsEsportsOutline' },
  ];

  readonly matches = signal<Match[]>([
    {
      id: 'mci-liv',
      sport: 'Fútbol',
      league: 'Premier League',
      home: 'Manchester City',
      away: 'Liverpool',
      homeShort: 'MCI',
      awayShort: 'LIV',
      homeScore: 2,
      awayScore: 1,
      minute: 68,
      status: 'EN VIVO',
      startsAt: 'Hoy, 20:00',
      odds: { local: 1.65, draw: 3.8, away: 4.2 },
      drawMarket: true,
    },
    {
      id: 'rma-bar',
      sport: 'Fútbol',
      league: 'LaLiga',
      home: 'Real Madrid',
      away: 'Barcelona',
      homeShort: 'RMA',
      awayShort: 'BAR',
      homeScore: 1,
      awayScore: 1,
      minute: 54,
      status: 'EN VIVO',
      startsAt: 'Hoy, 21:00',
      odds: { local: 2.1, draw: 3.4, away: 3.1 },
      drawMarket: true,
    },
    {
      id: 'ars-che',
      sport: 'Fútbol',
      league: 'Premier League',
      home: 'Arsenal',
      away: 'Chelsea',
      homeShort: 'ARS',
      awayShort: 'CHE',
      homeScore: 0,
      awayScore: 0,
      minute: 0,
      status: 'PRÓXIMO',
      startsAt: 'Hoy, 22:30',
      odds: { local: 1.9, draw: 3.6, away: 4.1 },
      drawMarket: true,
    },
    {
      id: 'bay-dor',
      sport: 'Fútbol',
      league: 'Bundesliga',
      home: 'Bayern Múnich',
      away: 'Borussia Dortmund',
      homeShort: 'BAY',
      awayShort: 'BVB',
      homeScore: 3,
      awayScore: 2,
      minute: 90,
      status: 'FINALIZADO',
      startsAt: 'Hoy, 18:30',
      odds: { local: 1.75, draw: 4.0, away: 3.9 },
      drawMarket: true,
    },
  ]);

  readonly bets = signal<Bet[]>([
    {
      id: 'b-001',
      matchId: 'mci-liv',
      match: 'Manchester City vs Liverpool',
      market: 'Resultado final',
      selection: 'Manchester City',
      stake: 50000,
      odds: 1.65,
      potential: 82500,
      status: 'PENDIENTE',
      placedAt: 'Hoy, 20:12',
      outcome: null,
    },
    {
      id: 'b-002',
      matchId: 'rma-bar',
      match: 'Real Madrid vs Barcelona',
      market: 'Más de 2.5 goles',
      selection: 'Más de 2.5',
      stake: 25000,
      odds: 1.9,
      potential: 47500,
      status: 'GANADA',
      placedAt: 'Ayer, 21:05',
      outcome: {
        winner: 'LOCAL',
        winnerName: 'Real Madrid',
        homeScore: 2,
        awayScore: 1,
        finished: true,
      },
    },
    {
      id: 'b-003',
      matchId: 'ars-che',
      match: 'Arsenal vs Chelsea',
      market: 'Resultado final',
      selection: 'Chelsea',
      stake: 20000,
      odds: 3.2,
      potential: 64000,
      status: 'PERDIDA',
      placedAt: 'Ayer, 19:40',
      outcome: {
        winner: 'LOCAL',
        winnerName: 'Arsenal',
        homeScore: 2,
        awayScore: 0,
        finished: true,
      },
    },
  ]);

  readonly wallet = signal<Wallet>({ balance: 1477500, heldBalance: 0, currency: 'COP' });

  readonly movements = signal<Movement[]>([
    {
      id: 'mv-001',
      concept: 'Apuesta Manchester City vs Liverpool',
      date: 'Hoy, 20:12',
      amount: -50000,
      type: 'GASTO',
    },
    {
      id: 'mv-002',
      concept: 'Ganancia Real Madrid vs Barcelona',
      date: 'Ayer, 23:10',
      amount: 47500,
      type: 'GANANCIA',
    },
    {
      id: 'mv-003',
      concept: 'Apuesta Arsenal vs Chelsea',
      date: 'Ayer, 19:40',
      amount: -20000,
      type: 'GASTO',
    },
    {
      id: 'mv-004',
      concept: 'Depósito',
      date: 'Ayer, 18:00',
      amount: 500000,
      type: 'INGRESO',
    },
    {
      id: 'mv-005',
      concept: 'Saldo inicial',
      date: '10 nov, 09:00',
      amount: 1000000,
      type: 'INGRESO',
    },
  ]);

  readonly serverNotifications = signal<AppNotification[]>([]);

  refreshNotifications(): void {
    this.api.notifications().subscribe({
      next: (notifications) => this.serverNotifications.set(notifications),
      error: () => undefined,
    });
    this.api.readNotifications().subscribe({
      next: (ids) => this.readNotifications.set(ids),
      error: () => undefined,
    });
  }
  readonly notifications = computed<AppNotification[]>(() => this.serverNotifications());

  readonly readNotifications = signal<string[]>([]);

  readonly unreadNotifications = computed(
    () => this.notifications().filter((item) => !this.readNotifications().includes(item.id)).length,
  );

  isNotificationRead(id: string): boolean {
    return this.readNotifications().includes(id);
  }

  markNotificationRead(id: string): void {
    if (this.isNotificationRead(id)) {
      return;
    }
    this.readNotifications.update((ids) => [...ids, id]);
    this.api.markNotificationsRead([id]).subscribe({ error: () => this.rollbackRead(id) });
  }

  markAllNotificationsRead(): void {
    const pending = this.notifications()
      .map((item) => item.id)
      .filter((id) => !this.isNotificationRead(id));
    if (pending.length === 0) {
      return;
    }
    this.readNotifications.update((ids) => [...ids, ...pending]);
    this.api.markNotificationsRead(pending).subscribe({
      error: () => this.readNotifications.update((ids) => ids.filter((id) => !pending.includes(id))),
    });
  }

  private rollbackRead(id: string): void {
    this.readNotifications.update((ids) => ids.filter((current) => current !== id));
  }

  matchById(id: string): Match | undefined {
    return this.matches().find((match) => match.id === id);
  }

  readonly selectedMatch = signal<Match | null>(this.matches()[0] ?? null);
  readonly selectedSelection = signal<Selection>('LOCAL');
  readonly stake = signal(50000);

  selectMatch(match: Match, selection: Selection): void {
    this.selectedMatch.set(match);
    this.selectedSelection.set(
      selection === 'EMPATE' && !match.drawMarket ? 'LOCAL' : selection,
    );
  }

  updateStake(value: number): void {
    this.stake.set(Math.max(1000, Math.min(500000, value)));
  }

  potentialReturn(): number {
    const match = this.selectedMatch();
    if (!match) {
      return 0;
    }
    const odds =
      this.selectedSelection() === 'LOCAL'
        ? match.odds.local
        : this.selectedSelection() === 'EMPATE'
          ? match.odds.draw
          : match.odds.away;
    return Number((this.stake() * odds).toFixed(2));
  }
}
