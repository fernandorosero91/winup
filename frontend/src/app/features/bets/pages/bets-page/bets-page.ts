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

  readonly settled = computed(() =>
    this.data.bets().filter((bet) => bet.status !== 'PENDIENTE'),
  );

  readonly totalReturned = computed(() =>
    this.settled().reduce((total, bet) => total + this.displayReturn(bet), 0),
  );

  readonly netResult = computed(() => this.totalReturned() - this.totalStake());

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
    switch (bet.status) {
      case 'ANULADA':
        return 'Importe devuelto';
      case 'PENDIENTE':
        return 'Retorno potencial';
      default:
        return 'Retorno';
    }
  }

  displayReturn(bet: Bet): number {
    switch (bet.status) {
      case 'PERDIDA':
        return 0;
      case 'ANULADA':
        return bet.stake;
      default:
        return bet.potential;
    }
  }

  netLabel(bet: Bet): string {
    switch (bet.status) {
      case 'GANADA':
        return 'Ganancia neta';
      case 'PERDIDA':
        return 'Pérdida';
      case 'ANULADA':
        return 'Neto';
      default:
        return 'Ganancia potencial';
    }
  }

  netValue(bet: Bet): number {
    switch (bet.status) {
      case 'PERDIDA':
        return -bet.stake;
      case 'ANULADA':
        return 0;
      default:
        return bet.potential - bet.stake;
    }
  }
}
