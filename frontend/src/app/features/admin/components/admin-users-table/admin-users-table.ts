import { Component, input, output } from '@angular/core';
import { AdminUser } from '../../../../core/api/admin-api';

@Component({
  selector: 'app-admin-users-table',
  templateUrl: './admin-users-table.html',
  styleUrl: '../../pages/admin-page/admin-page.css',
})
export class AdminUsersTable {
  readonly users = input<AdminUser[]>([]);
  readonly roleChange = output<{ user: AdminUser; role: string }>();
}
