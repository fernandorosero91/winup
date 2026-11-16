import { Component, inject, signal } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { WinupData } from '../../../../core/data/winup-data';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';
import { formatCop } from '../../../../shared/money/format-money';

@Component({
  selector: 'app-bet-slip',
  imports: [NgIcon, EmptyState],
  templateUrl: './bet-slip.html',
  styleUrl: './bet-slip.css',
})
export class BetSlip {
  readonly data = inject(WinupData);
  readonly quick = [500, 1000, 5000, 10000, 25000, 50000, 100000];
  readonly placed = signal(false);
  readonly placing = signal(false);
  readonly error = signal<string | null>(null);

  selectionLabel(): string {
    const match = this.data.selectedMatch();
    if (!match) {
      return '';
    }
    const selection = this.data.selectedSelection();
    return selection === 'LOCAL'
      ? match.home
      : selection === 'EMPATE'
        ? 'Empate'
        : match.away;
  }

  selectedOdds(): number {
    const match = this.data.selectedMatch();
    if (!match) {
      return 0;
    }
    const selection = this.data.selectedSelection();
    return selection === 'LOCAL'
      ? match.odds.local
      : selection === 'EMPATE'
        ? match.odds.draw
        : match.odds.away;
  }

  onStake(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    if (Number.isFinite(value)) {
      this.data.updateStake(value);
    }
  }

  clear(): void {
    this.data.selectedMatch.set(null);
    this.placed.set(false);
  }

  place(): void {
    if (!this.data.selectedMatch() || this.placing()) {
      return;
    }
    this.placing.set(true);
    this.error.set(null);
    this.data.placeBet().subscribe({
      next: () => {
        this.placing.set(false);
        this.placed.set(true);
        setTimeout(() => this.placed.set(false), 2500);
      },
      error: () => {
        this.placing.set(false);
        this.error.set('No se pudo registrar la apuesta. Inténtalo de nuevo.');
      },
    });
  }

  readonly money = formatCop;
}
