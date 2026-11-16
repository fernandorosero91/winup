import { Component, input, output } from '@angular/core';
import { AdminDashboard } from '../../../../core/api/admin-api';
import { formatCop } from '../../../../shared/money/format-money';

@Component({
  selector: 'app-admin-summary',
  templateUrl: './admin-summary.html',
  styleUrl: './admin-summary.css',
})
export class AdminSummary {
  readonly dashboard = input<AdminDashboard | null>(null);
  readonly actionLoading = input(false);
  readonly ensure = output<void>();
  readonly seed = output<void>();
  moneyString(value: string): string {
    return formatCop(Number(value));
  }
}
