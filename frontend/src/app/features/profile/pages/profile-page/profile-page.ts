import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { AuthStore } from '../../../../core/auth/auth-store';
import { WinupData } from '../../../../core/data/winup-data';
import { ThemeMode, ThemeStore } from '../../../../core/theme/theme-store';
import { formatCop } from '../../../../shared/money/format-money';

@Component({
  selector: 'app-profile-page',
  imports: [NgIcon, RouterLink],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.css',
})
export class ProfilePage {
  readonly theme = inject(ThemeStore);
  readonly data = inject(WinupData);
  readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  readonly confirmLogout = signal(false);
  readonly userName = computed(() => this.auth.user()?.displayName ?? 'Usuario');
  readonly userEmail = computed(() => this.auth.user()?.email ?? '');
  readonly initials = computed(() => this.auth.initials());

  readonly themes: { mode: ThemeMode; label: string; icon: string; detail: string }[] = [
    { mode: 'system', label: 'Sistema', icon: 'lucideSmartphone', detail: 'Sigue la preferencia del dispositivo' },
    { mode: 'light', label: 'Claro', icon: 'lucideSun', detail: 'Fondo claro y tarjetas blancas' },
    { mode: 'dark', label: 'Oscuro', icon: 'lucideMoon', detail: 'Fondo azul oscuro y tarjetas elevadas' },
  ];

  setTheme(mode: ThemeMode): void {
    this.theme.setMode(mode);
  }

  requestLogout(): void {
    this.confirmLogout.set(true);
  }

  cancelLogout(): void {
    this.confirmLogout.set(false);
  }

  logout(): void {
    this.auth.clear();
    this.confirmLogout.set(false);
    this.router.navigate(['/login']);
  }

  readonly money = formatCop;
}
