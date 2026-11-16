import { toBet, toMovement } from './winup-api';

describe('WinupApi mappers', () => {
  it('parses money strings from a bet response', () => {
    const bet = toBet({
      id: 'b-1',
      match: 'Liverpool vs Arsenal',
      market: 'Resultado final',
      selection: 'Liverpool',
      stake: '50000.00',
      odds: 2.1,
      potential: '105000.00',
      status: 'PENDIENTE',
      placedAt: '10:00',
    });

    expect(bet.stake).toBe(50000);
    expect(bet.potential).toBe(105000);
    expect(bet.odds).toBe(2.1);
  });

  it('parses the movement amount from a wallet response', () => {
    const movement = toMovement({
      id: 'm-1',
      concept: 'Apuesta',
      date: '10:00',
      amount: '-5000.00',
      type: 'GASTO',
    });

    expect(movement.amount).toBe(-5000);
  });
});
