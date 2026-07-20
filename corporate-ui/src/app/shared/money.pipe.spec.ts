import { describe, expect, it } from 'vitest';
import { MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();

  it('returns empty string for null/undefined', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
  });

  it('formats paise as rupees with no decimals', () => {
    const out = pipe.transform(179500);
    expect(out).toContain('1,795');
    expect(out).not.toContain('.');
  });

  it('rounds to whole units (maximumFractionDigits 0)', () => {
    const out = pipe.transform(179599);
    expect(out).toContain('1,796');
    expect(out).not.toContain('.');
  });

  it('respects the currency argument', () => {
    const out = pipe.transform(100000, 'USD');
    expect(out).toContain('1,000');
  });
});
