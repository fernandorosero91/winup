import { Component, computed, inject, signal } from '@angular/core';
import { Bet, WinupData } from '../../../../core/data/winup-data';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';
import { formatCop } from '../../../../shared/money/format-money';

type BetFilter = 'TODAS' | 'PENDIENTE' | 'GANADA' | 'PERDIDA' | 'ANULADA';

@Component({
  selector: 'app-bets-page',
  imports: [EmptyState],
  templateUrl: './bets-page.html',
  styleUrl: './bets-page.css',
})
export class BetsPage {
  readonly data = inject(WinupData);
  readonly filter = signal<BetFilter>('TODAS');
  readonly tabs: BetFilter[] = ['TODAS', 'PENDIENTE', 'GANADA', 'PERDIDA', 'ANULADA'];

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

  tabLabel(tab: BetFilter): string {
    switch (tab) {
      case 'PENDIENTE':
        return 'Pendientes';
      case 'GANADA':
        return 'Ganadas';
      case 'PERDIDA':
        return 'Perdidas';
      case 'ANULADA':
        return 'Anuladas';
      default:
        return 'Todas';
    }
  }

  statusLabel(bet: Bet): string {
    switch (bet.status) {
      case 'PENDIENTE':
        return 'Pendiente';
      case 'GANADA':
        return 'Ganada';
      case 'ANULADA':
        return 'Anulada';
      default:
        return 'Perdida';
    }
  }

  returnLabel(bet: Bet): string {
    if (bet.status === 'ANULADA') {
      return 'Importe devuelto';
    }
    return bet.status === 'GANADA' ? 'Ganancia' : 'Retorno potencial';
  }
}
