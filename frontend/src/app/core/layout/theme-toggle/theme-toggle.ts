import { Component, inject } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { ThemeStore } from '../../theme/theme-store';

@Component({
  selector: 'app-theme-toggle',
  imports: [NgIcon],
  templateUrl: './theme-toggle.html',
  styleUrl: './theme-toggle.css',
})
export class ThemeToggle {
  readonly theme = inject(ThemeStore);
}
