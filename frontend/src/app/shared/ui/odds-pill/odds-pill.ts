import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-odds-pill',
  imports: [],
  templateUrl: './odds-pill.html',
  styleUrl: './odds-pill.css',
})
export class OddsPill {
  readonly label = input('1');
  readonly value = input(0);
  readonly selected = input(false);
  readonly pick = output<void>();
}
