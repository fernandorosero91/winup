import { Component, input } from '@angular/core';
import { NgIcon } from '@ng-icons/core';

@Component({
  selector: 'app-empty-state',
  imports: [NgIcon],
  templateUrl: './empty-state.html',
  styleUrl: './empty-state.css',
})
export class EmptyState {
  readonly icon = input('lucideInfo');
  readonly title = input('Sin contenido');
  readonly description = input('Todavía no hay nada que mostrar.');
}
