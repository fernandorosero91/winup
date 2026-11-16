import type { AppNotification, Bet, Movement } from '../data/winup-data';
import type { BetResponse, MovementResponse, NotificationResponse } from './winup-api.types';

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
