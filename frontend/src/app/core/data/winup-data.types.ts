export type MatchStatus = 'EN VIVO' | 'PRÓXIMO' | 'FINALIZADO' | 'SUSPENDIDO' | 'ANULADO';
export type StatusFilter = 'TODOS' | 'EN VIVO' | 'PRÓXIMO' | 'FINALIZADO' | 'ANULADO';
export type Selection = 'LOCAL' | 'EMPATE' | 'VISITANTE';
export type BetStatus = 'PENDIENTE' | 'GANADA' | 'PERDIDA' | 'ANULADA';
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
  income: number;
  expenses: number;
  movementCount: number;
}

export interface AppNotification {
  id: string;
  kind: NotificationKind;
  title: string;
  detail: string;
  date: string;
  icon: string;
}
