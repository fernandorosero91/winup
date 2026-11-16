import { Component, input } from '@angular/core';

export type MatchStatus = 'EN VIVO' | 'PRÓXIMO' | 'FINALIZADO' | 'SUSPENDIDO';

@Component({
  selector: 'app-status-chip',
  imports: [],
  templateUrl: './status-chip.html',
  styleUrl: './status-chip.css',
})
export class StatusChip {
  readonly status = input<MatchStatus>('PRÓXIMO');
}
