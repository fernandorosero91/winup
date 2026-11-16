import { formatCop } from './format-money';

describe('formatCop', () => {
  it('formats an amount in Colombian pesos', () => {
    expect(formatCop(100000).replace(/\u00a0/g, ' ')).toBe('$ 100.000,00');
  });

  it('formats zero with two decimals', () => {
    expect(formatCop(0).replace(/\u00a0/g, ' ')).toBe('$ 0,00');
  });

  it('formats a decimal amount with Colombian separators', () => {
    expect(formatCop(1234567.5).replace(/\u00a0/g, ' ')).toBe('$ 1.234.567,50');
  });
});
