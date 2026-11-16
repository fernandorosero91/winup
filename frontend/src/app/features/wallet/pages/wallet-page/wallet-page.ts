import { Component, computed, inject, signal } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { Movement, WinupData } from '../../../../core/data/winup-data';
import { formatCop } from '../../../../shared/money/format-money';
import { DepositPanel } from '../../components/deposit-panel/deposit-panel';
import { WithdrawPanel } from '../../components/withdraw-panel/withdraw-panel';

type WalletPanel = 'deposit' | 'withdraw' | null;

@Component({
  selector: 'app-wallet-page',
  imports: [NgIcon, DepositPanel, WithdrawPanel],
  templateUrl: './wallet-page.html',
  styleUrl: './wallet-page.css',
})
export class WalletPage {
  readonly data = inject(WinupData);

  readonly panel = signal<WalletPanel>(null);
  readonly message = signal<string | null>(null);

  readonly income = computed(() =>
    this.data.movements()
      .filter((movement) => movement.amount > 0)
      .reduce((total, movement) => total + movement.amount, 0),
  );

  readonly expenses = computed(() =>
    this.data.movements()
      .filter((movement) => movement.amount < 0)
      .reduce((total, movement) => total + Math.abs(movement.amount), 0),
  );

  readonly money = formatCop;

  iconFor(movement: Movement): string {
    if (movement.type === 'EGRESO') {
      return 'lucideBanknote';
    }
    return movement.type === 'GANANCIA'
      ? 'lucideCircleCheck'
      : movement.type === 'INGRESO'
        ? 'lucideArrowUpRight'
        : 'lucideTicket';
  }

  signedMoney(value: number): string {
    return `${value >= 0 ? '+' : '-'}${this.money(Math.abs(value))}`;
  }

  openDeposit(): void {
    this.message.set(null);
    this.panel.set('deposit');
  }

  openWithdraw(): void {
    this.message.set(null);
    this.panel.set('withdraw');
  }

  closePanel(): void {
    this.panel.set(null);
  }

  onFinished(message: string): void {
    this.panel.set(null);
    this.message.set(message);
  }
}
