import { Component, OnInit, inject, signal } from '@angular/core';
import { WinupApi } from '../../../../core/api/winup-api';
import { Match, WinupData } from '../../../../core/data/winup-data';
import { NgIcon } from '@ng-icons/core';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';

@Component({
  selector: 'app-results-page',
  imports: [NgIcon, EmptyState],
  templateUrl: './results-page.html',
  styleUrl: './results-page.css',
})
export class ResultsPage implements OnInit {
  private readonly api = inject(WinupApi);
  readonly data = inject(WinupData);

  readonly sportFilter = signal('TODOS');
  readonly matches = signal<Match[]>([]);
  readonly total = signal(0);
  readonly hasMore = signal(false);
  readonly loading = signal(true);
  readonly loadingMore = signal(false);

  readonly sportChips = [
    { name: 'TODOS', label: 'Todos', icon: 'matGridViewOutline' },
    ...this.data.sports,
  ];

  ngOnInit(): void {
    this.load(9, true);
  }

  pickSport(sport: string): void {
    if (this.sportFilter() === sport) {
      return;
    }
    this.sportFilter.set(sport);
    this.load(9, true);
  }

  loadMore(): void {
    if (this.loadingMore() || !this.hasMore()) {
      return;
    }
    this.load(Math.min(50, this.matches().length + 9), false);
  }

  private load(size: number, initial: boolean): void {
    if (initial) {
      this.loading.set(true);
    } else {
      this.loadingMore.set(true);
    }
    this.api
      .matches({ status: 'FINALIZADO', sport: this.sportFilter(), page: 0, size })
      .subscribe({
        next: (page) => {
          this.matches.set(page.items);
          this.total.set(page.total);
          this.hasMore.set(page.hasMore);
          this.loading.set(false);
          this.loadingMore.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.loadingMore.set(false);
        },
      });
  }
}
