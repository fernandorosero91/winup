import { Component, inject, output, signal } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { WinupApi } from '../../../../core/api/winup-api';
import { WinupData } from '../../../../core/data/winup-data';
import { formatCop } from '../../../../shared/money/format-money';

@Component({
  selector: 'app-deposit-panel',
  imports: [NgIcon],
  templateUrl: './deposit-panel.html',
  styleUrl: './deposit-panel.css',
})
export class DepositPanel {
  private readonly api = inject(WinupApi);
  private readonly data = inject(WinupData);

  readonly closed = output<void>();
  readonly finished = output<string>();

  readonly amount = signal(50000);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly quickDeposits = [10000, 25000, 50000, 100000];
  readonly money = formatCop;

  setAmount(value: number): void {
    this.amount.set(value);
  }

  updateAmount(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    if (Number.isFinite(value)) {
      this.amount.set(Math.max(5000, Math.min(500000, value)));
    }
  }

  confirm(): void {
    if (this.loading()) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.api.createDeposit(this.amount(), `${window.location.origin}/cartera`).subscribe({
      next: (deposit) => {
        if (deposit.checkoutUrl && deposit.checkoutUrl.startsWith('http')) {
          window.location.assign(deposit.checkoutUrl);
          this.loading.set(false);
          this.closed.emit();
          return;
        }
        this.api.simulateDeposit(deposit.id).subscribe({
          next: () => {
            this.loading.set(false);
            this.data.refresh();
            this.closed.emit();
            this.finished.emit('Depósito aprobado y acreditado.');
          },
          error: () => {
            this.loading.set(false);
            this.error.set('No se pudo confirmar el depósito simulado.');
          },
        });
      },
      error: (error) => {
        this.loading.set(false);
        this.error.set(
          error?.error?.message ?? 'No se pudo crear el depósito. Inténtalo de nuevo.',
        );
      },
    });
  }
}
