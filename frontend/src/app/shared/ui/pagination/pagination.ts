import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

const PAGE_WINDOW = 3;

@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.html',
  styleUrl: './pagination.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Pagination {
  readonly page = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly disabled = input(false);

  readonly pageChange = output<number>();

  readonly visiblePages = computed(() => {
    const total = this.totalPages();
    const half = Math.floor(PAGE_WINDOW / 2);
    let start = Math.max(0, this.page() - half);
    const end = Math.min(total, start + PAGE_WINDOW);
    start = Math.max(0, end - PAGE_WINDOW);
    return Array.from({ length: end - start }, (_, index) => start + index);
  });

  readonly canGoBack = computed(() => this.page() > 0 && !this.disabled());
  readonly canGoForward = computed(() => this.page() < this.totalPages() - 1 && !this.disabled());

  goTo(page: number): void {
    this.pageChange.emit(page);
  }
}
