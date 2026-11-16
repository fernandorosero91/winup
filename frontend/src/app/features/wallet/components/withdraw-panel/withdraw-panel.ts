import { Component, computed, inject, output, signal } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { WinupApi, WithdrawalResponse } from '../../../../core/api/winup-api';
import { WinupData } from '../../../../core/data/winup-data';
import { formatCop } from '../../../../shared/money/format-money';

@Component({
  selector: 'app-withdraw-panel',
  imports: [NgIcon],
  templateUrl: './withdraw-panel.html',
  styleUrl: './withdraw-panel.css',
})
export class WithdrawPanel {
  private readonly api = inject(WinupApi);
  private readonly data = inject(WinupData);

  readonly closed = output<void>();
  readonly finished = output<string>();

  readonly amount = signal(20000);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly quickAmounts = [20000, 50000, 100000, 200000];
  readonly money = formatCop;
  readonly available = computed(() => this.data.wallet().balance);

  setAmount(value: number): void {
    const limit = Math.max(5000, this.available());
    this.amount.set(Math.min(value, limit));
  }

  updateAmount(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    if (Number.isFinite(value)) {
      this.setAmount(value);
    }
  }

  confirm(): void {
    if (this.loading()) {
      return;
    }
    const value = this.amount();
    if (value < 5000) {
      this.error.set('El retiro mínimo es de $ 5.000,00.');
      return;
    }
    if (value > this.available()) {
      this.error.set('El importe supera tu saldo disponible.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.api.createWithdrawal(value).subscribe({
      next: (withdrawal) => {
        this.loading.set(false);
        this.data.refresh();
        this.closed.emit();
        this.finished.emit(this.messageFor(withdrawal));
      },
      error: (error) => {
        this.loading.set(false);
        this.error.set(
          error?.error?.message ?? 'No se pudo crear el retiro. Inténtalo de nuevo.',
        );
      },
    });
  }

  private messageFor(withdrawal: WithdrawalResponse): string {
    switch (withdrawal.status) {
      case 'PAID':
        return 'Retiro pagado. Tu saldo ya está actualizado.';
      case 'PENDING_APPROVAL':
        return withdrawal.message ?? 'Tu retiro quedó en revisión.';
      case 'FAILED':
        return withdrawal.message ?? 'No se pudo procesar el retiro. El saldo fue liberado.';
      case 'REJECTED':
        return withdrawal.message ?? 'El retiro fue rechazado. El saldo fue liberado.';
      default:
        return withdrawal.message ?? 'Tu retiro está en proceso.';
    }
  }
}
