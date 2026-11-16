import { Component, input } from '@angular/core';

@Component({
  selector: 'app-brand-logo',
  imports: [],
  templateUrl: './brand-logo.html',
  styleUrl: './brand-logo.css',
})
export class BrandLogo {
  readonly size = input<'sm' | 'md' | 'lg'>('md');
  readonly tagline = input(false);
  readonly variant = input<'auto' | 'dark'>('auto');
}
