import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthStore } from '../auth/auth-store';
import { readCache, writeCache } from './winup-cache';
import { Observable, tap, throwError } from 'rxjs';
import { WinupApi, toMovement } from '../api/winup-api';
import type { RealtimeMatchEvent } from '../realtime/realtime.service';
import {
  DEMO_BETS,
  DEMO_MATCHES,
  DEMO_MOVEMENTS,
  DEMO_WALLET,
  SPORTS,
} from './winup-data.fixtures';
import type { AppNotification, Bet, Match, Movement, Selection, StatusFilter, Wallet } from './winup-data.types';

export * from './winup-data.types';

type FetchMode = 'initial' | 'page' | 'background';

const PAGE_SIZE = 24;
const MOVEMENTS_PAGE_SIZE = 20;

@Injectable({ providedIn: 'root' })
export class WinupData {
  private readonly api = inject(WinupApi);
  private readonly auth = inject(AuthStore);
  private requestId = 0;

  constructor() {
    const user = this.auth.user()?.id;
    if (!user) {
      return;
    }
    const wallet = readCache<Wallet>(`wallet.${user}`);
    const bets = readCache<Bet[]>(`bets.${user}`);
    const movements = readCache<{
      items: Movement[];
      page: number;
      total: number;
      hasNext: boolean;
    }>(`movements.${user}`);
    if (wallet) {
      this.wallet.set(wallet);
    }
    if (bets) {
      this.bets.set(bets);
    }
    if (movements) {
      this.movements.set(movements.items);
      this.movementPage.set(movements.page);
      this.movementTotal.set(movements.total);
      this.movementHasNext.set(movements.hasNext);
    }
  }

  refresh(): void {
    this.fetch(this.page(), PAGE_SIZE, 'background');
    this.api.bets().subscribe({
      next: (bets) => {
        this.bets.set(bets);
        writeCache(`bets.${this.cacheUser()}`, bets);
      },
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
        const nextWallet: Wallet = {
          balance: Number(wallet.balance),
          heldBalance: Number(wallet.heldBalance ?? 0),
          currency: wallet.currency,
          income: Number(wallet.income),
          expenses: Number(wallet.expenses),
          movementCount: wallet.movementCount,
        };
        this.wallet.set(nextWallet);
        writeCache(`wallet.${this.cacheUser()}`, nextWallet);
      },
      error: () => undefined,
    });
    this.api.movements(0, MOVEMENTS_PAGE_SIZE).subscribe({
      next: (page) => {
        const items = page.items.map(toMovement);
        this.movements.set(items);
        this.movementPage.set(page.page);
        this.movementTotal.set(page.totalElements);
        this.movementHasNext.set(page.hasNext);
        writeCache(`movements.${this.cacheUser()}`, {
          items,
          page: page.page,
          total: page.totalElements,
          hasNext: page.hasNext,
        });
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
    const current = this.matches().find((match) => match.id === event.matchId);
    if (!current) {
      return;
    }
    const nextStatus =
      event.status === 'CLOSED'
        ? 'FINALIZADO'
        : event.status === 'VOID'
          ? 'ANULADO'
          : event.status === 'OPEN'
            ? event.minute > 0
              ? 'EN VIVO'
              : 'PRÓXIMO'
            : current.status;
    if (nextStatus !== current.status) {
      this.fetch(this.page(), PAGE_SIZE, 'background');
      return;
    }
    this.matches.update((matches) =>
      matches.map((match) =>
        match.id === event.matchId
          ? {
              ...match,
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
    const cacheKey = `matches.${this.statusFilter()}.${this.sportFilter()}.${page}`;
    const cached = readCache<{ items: Match[]; total: number }>(cacheKey);
    if (cached && mode !== 'background') {
      this.matches.set(cached.items);
      this.total.set(cached.total);
      this.syncSelection();
      this.loading.set(false);
    }
    if (mode === 'initial' && !cached) {
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
          writeCache(cacheKey, { items: result.items, total: result.total });
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

  private cacheUser(): string {
    return this.auth.user()?.id ?? 'anon';
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

  readonly sports = SPORTS;

  readonly matches = signal<Match[]>(DEMO_MATCHES);

  readonly bets = signal<Bet[]>(DEMO_BETS);

  readonly wallet = signal<Wallet>(DEMO_WALLET);

  readonly movements = signal<Movement[]>(DEMO_MOVEMENTS);
  readonly movementPage = signal(0);
  readonly movementTotal = signal(0);
  readonly movementHasNext = signal(false);
  readonly movementsLoading = signal(false);

  readonly serverNotifications = signal<AppNotification[]>([]);

  loadMoreMovements(): void {
    if (this.movementsLoading() || !this.movementHasNext()) {
      return;
    }
    this.movementsLoading.set(true);
    this.api.movements(this.movementPage() + 1, MOVEMENTS_PAGE_SIZE).subscribe({
      next: (page) => {
        this.movements.update((current) => [...current, ...page.items.map(toMovement)]);
        this.movementPage.set(page.page);
        this.movementTotal.set(page.totalElements);
        this.movementHasNext.set(page.hasNext);
        writeCache(`movements.${this.cacheUser()}`, {
          items: this.movements(),
          page: page.page,
          total: page.totalElements,
          hasNext: page.hasNext,
        });
      },
      error: () => this.movementsLoading.set(false),
      complete: () => this.movementsLoading.set(false),
    });
  }

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

  clearNotifications(): void {
    this.api.clearNotifications().subscribe({
      next: () => {
        this.serverNotifications.set([]);
        this.readNotifications.set([]);
      },
      error: () => undefined,
    });
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
    this.stake.set(Math.max(500, Math.min(500000, value)));
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
