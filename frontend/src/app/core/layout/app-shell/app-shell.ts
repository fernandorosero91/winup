import { Component, OnDestroy, OnInit, computed, effect, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { AuthStore } from '../../auth/auth-store';
import { WinupData } from '../../data/winup-data';
import { RealtimeService } from '../../realtime/realtime.service';
import { BrandLogo } from '../../../shared/ui/brand-logo/brand-logo';
import { formatCop } from '../../../shared/money/format-money';
import { ThemeToggle } from '../theme-toggle/theme-toggle';
import { NotificationSound } from '../../notifications/notification-sound';
import type { AppNotification } from '../../data/winup-data';

interface NavItem {
  label: string;
  path: string;
  icon: string;
  dot?: boolean;
}

@Component({
  selector: 'app-app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgIcon, BrandLogo, ThemeToggle],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.css',
})
export class AppShell implements OnInit, OnDestroy {
  readonly data = inject(WinupData);
  readonly auth = inject(AuthStore);
  private readonly realtime = inject(RealtimeService);
  private readonly sound = inject(NotificationSound);
  private timer?: ReturnType<typeof setInterval>;
  private hideToast?: ReturnType<typeof setTimeout>;
  private notificationTimers: ReturnType<typeof setTimeout>[] = [];
  private lastNotificationId: string | null = null;
  private firstAnnouncement = true;

  readonly toast = signal<AppNotification | null>(null);

  constructor() {
    effect(() => {
      const notifications = this.data.notifications();
      if (notifications.length > 0) {
        this.announce(notifications[0]);
      }
    });
  }

  readonly userName = computed(() => this.auth.user()?.displayName ?? 'Usuario');
  readonly initials = computed(() => this.auth.initials());

  ngOnInit(): void {
    this.data.refresh();
    this.unlockSoundOnFirstGesture();
    this.realtime.connect(
      (event) => this.data.applyRealtime(event),
      () => this.refreshNotificationsAfterSignal(),
    );
    this.timer = setInterval(() => this.data.refresh(), 30000);
  }

  private refreshNotificationsAfterSignal(): void {
    this.notificationTimers.forEach((timer) => clearTimeout(timer));
    this.notificationTimers = [];
    this.data.refreshNotifications();
    [350, 900, 1800].forEach((delay) => {
      this.notificationTimers.push(
        setTimeout(() => this.data.refreshNotifications(), delay),
      );
    });
  }

  dismissToast(): void {
    this.toast.set(null);
  }

  private announce(notification: AppNotification): void {
    if (notification.id === this.lastNotificationId) {
      return;
    }
    const isFirst = this.firstAnnouncement;
    this.lastNotificationId = notification.id;
    this.firstAnnouncement = false;
    if (isFirst) {
      return;
    }
    this.sound.play(this.toneFor(notification));
    this.toast.set(notification);
    if (this.hideToast) {
      clearTimeout(this.hideToast);
    }
    const isResult = notification.kind === 'GANADA' || notification.kind === 'PERDIDA';
    this.hideToast = setTimeout(() => this.toast.set(null), isResult ? 9000 : 6000);
  }

  private toneFor(notification: AppNotification): 'WIN' | 'LOSS' | 'PROMO' {
    if (notification.kind === 'GANADA') {
      return 'WIN';
    }
    if (notification.kind === 'PERDIDA') {
      return 'LOSS';
    }
    return 'PROMO';
  }

  private unlockSoundOnFirstGesture(): void {
    const unlock = (): void => this.sound.unlock();
    document.addEventListener('pointerdown', unlock, { once: true });
    document.addEventListener('keydown', unlock, { once: true });
  }

  ngOnDestroy(): void {
    if (this.timer) {
      clearInterval(this.timer);
    }
    if (this.hideToast) {
      clearTimeout(this.hideToast);
    }
    this.notificationTimers.forEach((timer) => clearTimeout(timer));
    this.realtime.disconnect();
  }

  readonly nav: NavItem[] = [
    { label: 'Inicio', path: '/inicio', icon: 'lucideHouse' },
    { label: 'Deportes', path: '/deportes', icon: 'lucideTrophy' },
    { label: 'En Vivo', path: '/en-vivo', icon: 'lucideRadio', dot: true },
    { label: 'Mis Apuestas', path: '/mis-apuestas', icon: 'lucideTicket' },
    { label: 'Resultados', path: '/resultados', icon: 'lucideChartNoAxesColumn' },
    { label: 'Notificaciones', path: '/notificaciones', icon: 'lucideBell' },
    { label: 'Perfil', path: '/perfil', icon: 'lucideUser' },
  ];

  readonly mobileNav: NavItem[] = [
    { label: 'Inicio', path: '/inicio', icon: 'lucideHouse' },
    { label: 'En Vivo', path: '/en-vivo', icon: 'lucideRadio', dot: true },
    { label: 'Apuestas', path: '/mis-apuestas', icon: 'lucideTicket' },
    { label: 'Cartera', path: '/cartera', icon: 'lucideWallet' },
    { label: 'Perfil', path: '/perfil', icon: 'lucideUser' },
  ];

  readonly money = formatCop;
}
