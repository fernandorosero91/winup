import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { WinupApi } from '../api/winup-api';
import { WinupData } from './winup-data';

describe('WinupData', () => {
  let service: WinupData;
  let api: WinupApi;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(WinupData);
    api = TestBed.inject(WinupApi);
    vi.spyOn(api, 'matches').mockReturnValue(
      of({ items: [], page: 0, size: 24, total: 0, hasMore: false }),
    );
    vi.spyOn(api, 'bets').mockReturnValue(of([]));
    vi.spyOn(api, 'wallet').mockReturnValue(
      of({ balance: '100000.00',
        heldBalance: '0.00', currency: 'COP', movements: [] }),
    );
    vi.spyOn(api, 'notifications').mockReturnValue(of([]));
    vi.spyOn(api, 'readNotifications').mockReturnValue(of([]));
    vi.spyOn(api, 'markNotificationsRead').mockReturnValue(of(void 0));
  });

  it('maps money strings from the wallet response to numbers', () => {
    vi.mocked(api.wallet).mockReturnValue(
      of({
        balance: '100000.00',
        heldBalance: '0.00',
        currency: 'COP',
        movements: [
          {
            id: 'm-1',
            concept: 'Apuesta',
            date: '10:00',
            amount: '-5000.00',
            type: 'GASTO',
          },
        ],
      }),
    );

    service.refresh();

    expect(service.wallet().balance).toBe(100000);
    expect(service.wallet().currency).toBe('COP');
    expect(service.movements()[0].amount).toBe(-5000);
  });

  it('selects a match and calculates the potential return', () => {
    const match = service.matches()[0];
    service.selectMatch(match, 'LOCAL');
    service.updateStake(50000);

    expect(service.selectedMatch()?.id).toBe(match.id);
    expect(service.selectedSelection()).toBe('LOCAL');
    expect(service.potentialReturn()).toBe(82500);
  });

  it('clamps the stake between 1000 and 500000', () => {
    service.updateStake(0);
    expect(service.stake()).toBe(1000);

    service.updateStake(900000);
    expect(service.stake()).toBe(500000);
  });

  it('returns zero when no match is selected', () => {
    service.selectedMatch.set(null);
    expect(service.potentialReturn()).toBe(0);
  });

  it('calculates the away return for the second match', () => {
    const match = service.matches()[1];
    service.selectMatch(match, 'VISITANTE');
    service.updateStake(100000);

    expect(service.potentialReturn()).toBe(310000);
  });

  it('loads the notification feed persisted by the backend', () => {
    vi.mocked(api.notifications).mockReturnValue(
      of([
        {
          id: 'n-1',
          kind: 'PENDIENTE' as const,
          title: 'Apuesta en juego',
          detail: 'Gen.G vs T1 · pendiente de liquidación',
          date: '14:59',
          icon: 'lucideClock3',
        },
        {
          id: 'n-2',
          kind: 'GANADA' as const,
          title: 'Apuesta ganada',
          detail: 'Gen.G vs T1 · $ 9.250,00 acreditados en tu cartera',
          date: '13:32',
          icon: 'lucideTrendingUp',
        },
      ]),
    );

    service.refresh();

    const notifications = service.notifications();
    expect(notifications).toHaveLength(2);
    expect(notifications.map((item) => item.kind)).toEqual(['PENDIENTE', 'GANADA']);
    expect(notifications[1].detail).toContain('9.250,00');
  });

  it('tracks read notifications', () => {
    service.serverNotifications.set([
      { id: 'n-1', kind: 'GANADA', title: 'Apuesta ganada', detail: 'A', date: '10:00', icon: 'lucideTrendingUp' },
      { id: 'n-2', kind: 'PERDIDA', title: 'Apuesta perdida', detail: 'B', date: '10:01', icon: 'lucideCircleX' },
      { id: 'n-3', kind: 'PENDIENTE', title: 'Apuesta en juego', detail: 'C', date: '10:02', icon: 'lucideClock3' },
    ]);
    expect(service.unreadNotifications()).toBe(3);

    service.markNotificationRead('n-1');
    expect(service.unreadNotifications()).toBe(2);

    service.markAllNotificationsRead();
    expect(service.unreadNotifications()).toBe(0);
  });

  it('moves through pages and reports the visible range', () => {
    vi.mocked(api.matches).mockReturnValue(
      of({ items: [], page: 0, size: 24, total: 56, hasMore: true }),
    );

    service.setStatus('EN VIVO');

    expect(service.totalPages()).toBe(3);
    expect(service.rangeStart()).toBe(1);
    expect(service.rangeEnd()).toBe(24);

    service.nextPage();

    expect(api.matches).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 1, size: 24, status: 'EN VIVO' }),
    );
    expect(service.page()).toBe(1);
    expect(service.rangeStart()).toBe(25);
    expect(service.rangeEnd()).toBe(48);
  });

  it('returns to the first page when the sport filter changes', () => {
    vi.mocked(api.matches).mockReturnValue(
      of({ items: [], page: 0, size: 24, total: 56, hasMore: true }),
    );

    service.applyFilters('EN VIVO', 'TODOS');
    service.goToPage(2);
    service.setSport('Fútbol');

    expect(service.page()).toBe(0);
    expect(api.matches).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 0, sport: 'Fútbol' }),
    );
  });

  it('persists a read notification through the api', () => {
    service.markNotificationRead('bet-1');

    expect(service.isNotificationRead('bet-1')).toBe(true);
    expect(api.markNotificationsRead).toHaveBeenCalledWith(['bet-1']);
  });

  it('marks all pending notifications and persists them', () => {
    service.serverNotifications.set([
      { id: 'n-1', kind: 'GANADA', title: 'Apuesta ganada', detail: 'A', date: '10:00', icon: 'lucideTrendingUp' },
      { id: 'n-2', kind: 'PERDIDA', title: 'Apuesta perdida', detail: 'B', date: '10:01', icon: 'lucideCircleX' },
    ]);

    service.markAllNotificationsRead();

    expect(service.unreadNotifications()).toBe(0);
    expect(api.markNotificationsRead).toHaveBeenCalledWith(['n-1', 'n-2']);
  });

  it('restores the read ids persisted on the server', () => {
    vi.mocked(api.readNotifications).mockReturnValue(of(['bet-b-001', 'bet-b-002']));

    service.refresh();

    expect(service.isNotificationRead('bet-b-001')).toBe(true);
    expect(service.isNotificationRead('bet-b-002')).toBe(true);
    expect(service.isNotificationRead('bet-b-003')).toBe(false);
  });

  it('applies realtime score updates to the selected list', () => {    const match = service.matches()[0];

    service.applyRealtime({
      matchId: match.id,
      status: 'OPEN',
      home: match.home,
      away: match.away,
      homeScore: 3,
      awayScore: 2,
      minute: 74,
      winner: '',
    });

    expect(service.matchById(match.id)?.homeScore).toBe(3);
    expect(service.matchById(match.id)?.awayScore).toBe(2);
    expect(service.matchById(match.id)?.minute).toBe(74);
  });
});
