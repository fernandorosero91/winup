import type { Match } from '../data/winup-data';

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
