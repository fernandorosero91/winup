import { Component, computed, input, output } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { Match, Selection } from '../../../core/data/winup-data';
import { LiveBadge } from '../live-badge/live-badge';
import { OddsPill } from '../odds-pill/odds-pill';

@Component({
  selector: 'app-match-tile',
  imports: [NgIcon, LiveBadge, OddsPill],
  templateUrl: './match-tile.html',
  styleUrl: './match-tile.css',
})
export class MatchTile {
  readonly match = input.required<Match>();
  readonly selected = input<Selection | null>(null);
  readonly pick = output<{ match: Match; selection: Selection }>();

  readonly sourceLabel = computed(() => {
    const match = this.match();
    if (match.isSimulated ?? true) {
      return 'SIMULADO';
    }
    return match.source === 'API_FOOTBALL' || match.source === 'FOOTBALLDATA'
      ? 'API'
      : (match.source ?? 'SIMULADO');
  });
}
