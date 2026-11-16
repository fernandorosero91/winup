import { Component, OnInit, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import {
  Match,
  Selection,
  StatusFilter,
  WinupData,
} from '../../../../core/data/winup-data';
import { HeroBanner } from '../../../../shared/ui/hero-banner/hero-banner';
import { MatchTile } from '../../../../shared/ui/match-tile/match-tile';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';
import { BetSlip } from '../../../bet-slip/components/bet-slip/bet-slip';
import { Pagination } from '../../../../shared/ui/pagination/pagination';

type FeedMode = 'inicio' | 'en-vivo';

@Component({
  selector: 'app-live-page',
  imports: [RouterLink, NgIcon, HeroBanner, MatchTile, EmptyState, BetSlip, Pagination],
  templateUrl: './live-page.html',
  styleUrl: './live-page.css',
})
export class LivePage implements OnInit {
  readonly data = inject(WinupData);
  readonly mode = input<FeedMode>('inicio');

  readonly statusTabs: { label: string; value: StatusFilter }[] = [
    { label: 'Todos', value: 'TODOS' },
    { label: 'En vivo', value: 'EN VIVO' },
    { label: 'Próximos', value: 'PRÓXIMO' },
    { label: 'Finalizados', value: 'FINALIZADO' },
  ];

  readonly sportChips = [
    { name: 'TODOS', label: 'Todos', icon: 'matGridViewOutline' },
    ...this.data.sports,
  ];

  readonly heading = computed(() =>
    this.mode() === 'en-vivo' ? 'En vivo' : 'Partidos en vivo',
  );

  readonly listTitle = computed(() => {
    const sport = this.data.sportFilter();
    const status = this.data.statusFilter();
    if (status === 'EN VIVO') {
      return sport === 'TODOS' ? 'Partidos en vivo' : `${sport} en vivo`;
    }
    if (status === 'PRÓXIMO') {
      return 'Próximos partidos';
    }
    if (status === 'FINALIZADO') {
      return 'Resultados recientes';
    }
    return 'Todos los partidos';
  });

  readonly subtitle = computed(() =>
    this.mode() === 'en-vivo'
      ? 'Filtra por deporte y estado, y carga más partidos sin salir de la página.'
      : 'Lo que está pasando ahora mismo en todos los deportes.',
  );

  ngOnInit(): void {
    if (this.mode() === 'inicio') {
      if (this.data.statusFilter() !== 'EN VIVO' || this.data.sportFilter() !== 'TODOS') {
        this.data.applyFilters('EN VIVO', 'TODOS');
      }
      return;
    }
    if (this.data.statusFilter() === 'TODOS' && this.data.sportFilter() === 'TODOS') {
      this.data.setStatus('EN VIVO');
    }
  }

  selectionFor(match: Match): Selection | null {
    const selected = this.data.selectedMatch();
    return selected?.id === match.id ? this.data.selectedSelection() : null;
  }

  select(event: { match: Match; selection: Selection }): void {
    this.data.selectMatch(event.match, event.selection);
  }

  pickSport(sport: string): void {
    this.data.setSport(sport);
  }

  pickStatus(status: StatusFilter): void {
    this.data.setStatus(status);
  }

  reset(): void {
    this.data.resetFilters('EN VIVO');
  }
}
