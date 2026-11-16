import { Component, computed, inject, signal } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { WinupData } from '../../../../core/data/winup-data';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';

type NotificationFilter = 'TODAS' | 'NO LEIDAS';

@Component({
  selector: 'app-notifications-page',
  imports: [NgIcon, EmptyState],
  templateUrl: './notifications-page.html',
  styleUrl: './notifications-page.css',
})
export class NotificationsPage {
  readonly data = inject(WinupData);
  readonly filter = signal<NotificationFilter>('TODAS');

  readonly visible = computed(() =>
    this.filter() === 'TODAS'
      ? this.data.notifications()
      : this.data.notifications().filter((item) => !this.data.isNotificationRead(item.id)),
  );

  readonly won = computed(
    () => this.data.notifications().filter((item) => item.kind === 'GANADA').length,
  );

  readonly lost = computed(
    () => this.data.notifications().filter((item) => item.kind === 'PERDIDA').length,
  );

  readonly pending = computed(
    () => this.data.notifications().filter((item) => item.kind === 'PENDIENTE').length,
  );

  markAll(): void {
    this.data.markAllNotificationsRead();
  }

  markOne(id: string): void {
    this.data.markNotificationRead(id);
  }

  clearAll(): void {
    if (this.data.notifications().length === 0) {
      return;
    }
    this.data.clearNotifications();
  }
}
