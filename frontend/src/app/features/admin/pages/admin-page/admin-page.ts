import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import {
  AdminApi,
  AdminDashboard,
  AdminLedger,
  AdminPayment,
  AdminPromo,
  AdminUser,
} from '../../../../core/api/admin-api';
import { AuthStore } from '../../../../core/auth/auth-store';
import { WinupData, type Match } from '../../../../core/data/winup-data';
import { formatCop } from '../../../../shared/money/format-money';
import { AdminSummary } from '../../components/admin-summary/admin-summary';
import { Pagination } from '../../../../shared/ui/pagination/pagination';
import { AdminUsersTable } from '../../components/admin-users-table/admin-users-table';

type AdminTab = 'resumen' | 'usuarios' | 'promociones' | 'pagos' | 'partidos' | 'ledger';

@Component({
  selector: 'app-admin-page',
  imports: [AdminSummary, AdminUsersTable, Pagination],
  templateUrl: './admin-page.html',
  styleUrl: './admin-page.css',
})
export class AdminPage {
  private readonly api = inject(AdminApi);
  private readonly data = inject(WinupData);
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  readonly tab = signal<AdminTab>('resumen');
  readonly tabs = [
    { id: 'resumen', label: 'Resumen' },
    { id: 'usuarios', label: 'Usuarios' },
    { id: 'promociones', label: 'Promociones' },
    { id: 'pagos', label: 'Pagos' },
    { id: 'partidos', label: 'Partidos' },
    { id: 'ledger', label: 'Ledger' },
  ] as const;
  readonly dashboard = signal<AdminDashboard | null>(null);
  readonly users = signal<AdminUser[]>([]);
  readonly payments = signal<AdminPayment[]>([]);
  readonly ledger = signal<AdminLedger[]>([]);
  readonly promotions = signal<AdminPromo[]>([]);
  readonly promoTitle = signal('');
  readonly promoDetail = signal('');
  readonly promoScheduledAt = signal('');
  readonly sports = this.data.sports;
  readonly matchSport = signal('Fútbol');
  readonly matchLeague = signal('');
  readonly matchHome = signal('');
  readonly matchAway = signal('');
  readonly matchLocalOdds = signal(1.8);
  readonly matchDrawOdds = signal(3.5);
  readonly matchAwayOdds = signal(4.2);
  readonly matchMinute = signal(1);
  readonly matches = signal<Match[]>([]);
  readonly matchPage = signal(0);
  readonly matchPageSize = 20;
  readonly matchStatus = signal('TODOS');
  readonly matchTotal = signal(0);
  readonly matchStatuses = ['TODOS', 'EN VIVO', 'PRÓXIMO', 'FINALIZADO'] as const;
  readonly matchTotalPages = computed(() => Math.max(1, Math.ceil(this.matchTotal() / this.matchPageSize)));
  readonly matchRangeStart = computed(() =>
    this.matchTotal() === 0 ? 0 : this.matchPage() * this.matchPageSize + 1,
  );
  readonly matchRangeEnd = computed(() =>
    Math.min(this.matchTotal(), (this.matchPage() + 1) * this.matchPageSize),
  );
  readonly loading = signal(true);
  readonly actionLoading = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);

  constructor() {
    this.load();
  }

  reloadApp(): void {
    globalThis.location?.reload();
  }

  setTab(tab: AdminTab): void {
    this.tab.set(tab);
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.dashboard().subscribe({
      next: (dashboard) => this.dashboard.set(dashboard),
      error: (error: { status?: number }) => {
        this.loading.set(false);
        if (error?.status === 401 || error?.status === 403) {
          this.auth.clear();
          this.router.navigate(['/login']);
          return;
        }
        this.error.set('No se pudo cargar el panel.');
      },
    });
    this.api.users().subscribe({ next: (users) => this.users.set(users) });
    this.api.payments().subscribe({ next: (payments) => this.payments.set(payments) });
    this.api.ledger().subscribe({ next: (ledger) => this.ledger.set(ledger) });
    this.api.promotions().subscribe({ next: (promotions) => this.promotions.set(promotions) });
    this.loadMatches();
  }

  loadMatches(): void {
    this.api.matches(this.matchPage(), this.matchPageSize, this.matchStatus()).subscribe({
      next: (page) => {
        this.matches.set(page.items);
        this.matchTotal.set(page.total);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  goToMatchPage(page: number): void {
    const target = Math.max(0, Math.min(this.matchTotalPages() - 1, page));
    if (target === this.matchPage()) {
      return;
    }
    this.matchPage.set(target);
    this.loadMatches();
  }

  setMatchStatus(status: string): void {
    if (this.matchStatus() === status) {
      return;
    }
    this.matchStatus.set(status);
    this.matchPage.set(0);
    this.loadMatches();
  }

  matchDrawAvailable(): boolean {
    return this.drawSports().includes(this.matchSport());
  }

  createMatch(): void {
    const home = this.matchHome().trim();
    const away = this.matchAway().trim();
    if (!home || !away) {
      this.error.set('El partido necesita equipo local y visitante.');
      return;
    }
    if (home.toLowerCase() === away.toLowerCase()) {
      this.error.set('Los equipos deben ser distintos.');
      return;
    }
    this.actionLoading.set(true);
    this.api
      .createMatch({
        sport: this.matchSport(),
        league: this.matchLeague().trim(),
        home,
        away,
        localOdds: this.matchLocalOdds(),
        drawOdds: this.matchDrawAvailable() ? this.matchDrawOdds() : null,
        awayOdds: this.matchAwayOdds(),
        minute: this.matchMinute(),
      })
      .subscribe({
        next: (match) => {
          this.actionLoading.set(false);
          this.message.set(`Partido creado: ${match.home} vs ${match.away}.`);
          this.matchHome.set('');
          this.matchAway.set('');
          this.matchPage.set(0);
          this.matchStatus.set('TODOS');
          this.load();
        },
        error: (error: { status?: number }) => {
          this.actionLoading.set(false);
          this.error.set(
            error?.status === 403
              ? 'Tu sesión no tiene permisos de administrador. Cierra sesión y vuelve a entrar.'
              : 'No se pudo crear el partido. Revisa deporte y cuotas.',
          );
        },
      });
  }

  private drawSports(): string[] {
    return [
      'Fútbol',
      'AFL',
      'Balonmano',
      'Hockey',
      'NFL & NCAA',
      'Rugby',
    ];
  }

  updateRole(user: AdminUser, role: string): void {
    this.api.updateRole(user.id, role).subscribe({
      next: (updated) => {
        this.users.update((users) => users.map((item) => (item.id === updated.id ? updated : item)));
        this.message.set(`Rol de ${updated.email} actualizado a ${updated.role}.`);
      },
      error: () => this.error.set('No se pudo actualizar el rol.'),
    });
  }

  updatePromoTitle(value: string): void {
    this.promoTitle.set(value);
  }

  updatePromoDetail(value: string): void {
    this.promoDetail.set(value);
  }

  updatePromoSchedule(value: string): void {
    this.promoScheduledAt.set(value);
  }

  createPromotion(): void {
    const title = this.promoTitle().trim();
    const detail = this.promoDetail().trim();
    if (!title || !detail) {
      this.error.set('La promoción necesita título y mensaje.');
      return;
    }
    const schedule = this.promoScheduledAt();
    const scheduledAt = schedule ? new Date(schedule).toISOString() : null;
    this.actionLoading.set(true);
    this.api.createPromotion(title, detail, scheduledAt).subscribe({
      next: (promo) => {
        this.actionLoading.set(false);
        this.promoTitle.set('');
        this.promoDetail.set('');
        this.promoScheduledAt.set('');
        this.message.set(
          promo.status === 'SENT'
            ? 'Promoción enviada a todos los usuarios.'
            : 'Promoción programada correctamente.',
        );
        this.load();
      },
      error: () => {
        this.actionLoading.set(false);
        this.error.set('No se pudo crear la promoción.');
      },
    });
  }

  cancelPromotion(promo: AdminPromo): void {
    this.api.cancelPromotion(promo.id).subscribe({
      next: () => {
        this.message.set('Promoción cancelada.');
        this.load();
      },
      error: () => this.error.set('Solo se pueden cancelar promociones programadas.'),
    });
  }

  formatDate(value: string | null): string {
    if (!value) {
      return '—';
    }
    return new Intl.DateTimeFormat('es-CO', {
      dateStyle: 'short',
      timeStyle: 'short',
      timeZone: 'America/Bogota',
    }).format(new Date(value));
  }

  ensureSports(): void {
    this.actionLoading.set(true);
    this.api.ensureSports().subscribe({
      next: (result) => {
        this.message.set(`Deportes activos: ${result.activeSports}.`);
        this.actionLoading.set(false);
        this.load();
      },
      error: () => {
        this.actionLoading.set(false);
        this.error.set('No se pudieron asegurar los deportes.');
      },
    });
  }

  seed(): void {
    this.actionLoading.set(true);
    this.api.seed(300).subscribe({
      next: (result) => {
        this.message.set(`Partidos simulados creados: ${result.created}.`);
        this.actionLoading.set(false);
        this.load();
      },
      error: () => {
        this.actionLoading.set(false);
        this.error.set('No se pudieron generar partidos.');
      },
    });
  }

  money(value: string): string {
    return formatCop(Number(value));
  }

  shortId(value: string): string {
    return value.slice(0, 8);
  }
}
