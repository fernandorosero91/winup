import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { SportSummary, WinupApi } from '../../../../core/api/winup-api';
import { WinupData } from '../../../../core/data/winup-data';

@Component({
  selector: 'app-sports-page',
  imports: [NgIcon],
  templateUrl: './sports-page.html',
  styleUrl: './sports-page.css',
})
export class SportsPage implements OnInit {
  private readonly api = inject(WinupApi);
  private readonly router = inject(Router);
  readonly data = inject(WinupData);

  readonly summaries = signal<SportSummary[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.api.sports().subscribe({
      next: (summaries) => {
        this.summaries.set(summaries);
        this.loading.set(false);
      },
      error: () => {
        this.summaries.set(
          this.data.sports.map((sport) => ({
            name: sport.name,
            live: 0,
            upcoming: 0,
            finished: 0,
            total: 0,
          })),
        );
        this.loading.set(false);
      },
    });
  }

  iconFor(name: string): string {
    return this.data.sports.find((sport) => sport.name === name)?.icon ?? 'matGridViewOutline';
  }

  open(name: string): void {
    this.data.applyFilters('TODOS', name);
    this.router.navigate(['/en-vivo']);
  }

  openAll(): void {
    this.data.applyFilters('TODOS', 'TODOS');
    this.router.navigate(['/en-vivo']);
  }
}
