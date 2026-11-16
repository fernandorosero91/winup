import { Routes } from '@angular/router';
import { AppShell } from './core/layout/app-shell/app-shell';
import { authGuard } from './core/auth/auth.guard';
import { adminGuard } from './core/auth/admin.guard';

export const routes: Routes = [
  {
    path: 'login',
    data: { mode: 'login' },
    loadComponent: () =>
      import('./features/auth/pages/auth-page/auth-page').then((m) => m.AuthPage),
  },
  {
    path: 'registro',
    data: { mode: 'register' },
    loadComponent: () =>
      import('./features/auth/pages/auth-page/auth-page').then((m) => m.AuthPage),
  },
  {
    path: '',
    component: AppShell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        data: { mode: 'inicio' },
        loadComponent: () =>
          import('./features/live/pages/live-page/live-page').then((m) => m.LivePage),
      },
      {
        path: 'deportes',
        loadComponent: () =>
          import('./features/sports/pages/sports-page/sports-page').then((m) => m.SportsPage),
      },
      {
        path: 'en-vivo',
        data: { mode: 'en-vivo' },
        loadComponent: () =>
          import('./features/live/pages/live-page/live-page').then((m) => m.LivePage),
      },
      {
        path: 'mis-apuestas',
        loadComponent: () =>
          import('./features/bets/pages/bets-page/bets-page').then((m) => m.BetsPage),
      },
      {
        path: 'resultados',
        loadComponent: () =>
          import('./features/results/pages/results-page/results-page').then((m) => m.ResultsPage),
      },
      {
        path: 'cartera',
        loadComponent: () =>
          import('./features/wallet/pages/wallet-page/wallet-page').then((m) => m.WalletPage),
      },
      {
        path: 'perfil',
        loadComponent: () =>
          import('./features/profile/pages/profile-page/profile-page').then((m) => m.ProfilePage),
      },
      {
        path: 'notificaciones',
        loadComponent: () =>
          import('./features/notifications/pages/notifications-page/notifications-page').then(
            (m) => m.NotificationsPage,
          ),
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/pages/admin-page/admin-page').then((m) => m.AdminPage),
      },
      { path: '**', redirectTo: 'inicio' },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
