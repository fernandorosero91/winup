import { Component, input } from '@angular/core';

@Component({
  selector: 'app-live-badge',
  imports: [],
  templateUrl: './live-badge.html',
  styleUrl: './live-badge.css',
})
export class LiveBadge {
  readonly minute = input(0);
}
