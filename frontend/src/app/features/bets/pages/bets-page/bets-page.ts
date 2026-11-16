import { Component, computed, inject, signal } from '@angular/core';
import { Bet, WinupData } from '../../../../core/data/winup-data';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';
import { formatCop } from '../../../../shared/money/format-money';

type BetFilter = 'TODAS' | 'PENDIENTE' | 'GANADA' | 'PERDIDA';

@Component({
  selector: 'app-bets-page',
  imports: [EmptyState],
  templateUrl: './bets-page.html',
  styleUrl: './bets-page.css',
})
export class BetsPage {
  readonly data = inject(WinupData);
  readonly filter = signal<BetFilter>('TODAS');
  readonly tabs: BetFilter[] = ['TODAS', 'PENDIENTE', 'GANADA', 'PERDIDA'];

  readonly filtered = computed(() => {
    const value = this.filter();
    return value === 'TODAS'
      ? this.data.bets()
      : this.data.bets().filter((bet) => bet.status === value);
  });

  readonly totalStake = computed(() =>
    this.data.bets().reduce((total, bet) => total + bet.stake, 0),
  );

  readonly totalWon = computed(() =>
    this.data.bets()
      .filter((bet) => bet.status === 'GANADA')
      .reduce((total, bet) => total + bet.potential, 0),
  );

  readonly money = formatCop;

  statusLabel(bet: Bet): string {
    return bet.status === 'PENDIENTE'
      ? 'Pendiente'
      : bet.status === 'GANADA'
        ? 'Ganada'
        : 'Perdida';
  }
}
