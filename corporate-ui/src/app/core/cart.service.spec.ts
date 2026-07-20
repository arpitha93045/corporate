import { beforeEach, describe, expect, it } from 'vitest';
import { CartService } from './cart.service';
import { Branding, Product } from '../models/models';

function product(overrides: Partial<Product> = {}): Product {
  return {
    id: 1,
    name: 'Aromatherapy Candle Trio',
    slug: 'aromatherapy-candle-trio',
    description: 'Three soy-wax candles.',
    priceCents: 179500,
    imageUrl: null,
    inStock: true,
    categoryName: 'Home & Living',
    categorySlug: 'home-and-living',
    ...overrides,
  };
}

describe('CartService', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('starts empty', () => {
    const cart = new CartService();
    expect(cart.lines()).toEqual([]);
    expect(cart.count()).toBe(0);
    expect(cart.subtotalCents()).toBe(0);
  });

  it('adds a line and reflects count + subtotal', () => {
    const cart = new CartService();
    cart.add(product({ priceCents: 1000 }), 3);
    expect(cart.lines().length).toBe(1);
    expect(cart.count()).toBe(3);
    expect(cart.subtotalCents()).toBe(3000);
  });

  it('merges the same product with no branding', () => {
    const cart = new CartService();
    cart.add(product(), 1);
    cart.add(product(), 2);
    expect(cart.lines().length).toBe(1);
    expect(cart.count()).toBe(3);
  });

  it('keeps differing branding as separate lines', () => {
    const cart = new CartService();
    const a: Branding = { message: 'Happy Diwali', logoUrl: null };
    const b: Branding = { message: 'Season’s Greetings', logoUrl: null };
    cart.add(product(), 1, a);
    cart.add(product(), 1, b);
    expect(cart.lines().length).toBe(2);
    expect(cart.count()).toBe(2);
  });

  it('merges identical branding', () => {
    const cart = new CartService();
    const b: Branding = { message: 'Happy Diwali', logoUrl: 'https://x/y.png' };
    cart.add(product(), 1, { ...b });
    cart.add(product(), 2, { ...b });
    expect(cart.lines().length).toBe(1);
    expect(cart.count()).toBe(3);
  });

  it('normalizes whitespace-only branding to none (merges with the plain line)', () => {
    const cart = new CartService();
    cart.add(product(), 1);
    cart.add(product(), 1, { message: '   ', logoUrl: '  ' });
    expect(cart.lines().length).toBe(1);
    expect(cart.lines()[0].branding).toBeUndefined();
    expect(cart.count()).toBe(2);
  });

  it('trims branding values when storing', () => {
    const cart = new CartService();
    cart.add(product(), 1, { message: '  Hi  ', logoUrl: '  https://x/y.png  ' });
    expect(cart.lines()[0].branding).toEqual({ message: 'Hi', logoUrl: 'https://x/y.png' });
  });

  it('setQuantity(0) removes the line; positive updates it', () => {
    const cart = new CartService();
    cart.add(product(), 5);
    cart.setQuantity(0, 2);
    expect(cart.count()).toBe(2);
    cart.setQuantity(0, 0);
    expect(cart.lines()).toEqual([]);
  });

  it('remove() and clear() empty the cart', () => {
    const cart = new CartService();
    cart.add(product({ id: 1 }), 1);
    cart.add(product({ id: 2, slug: 'b' }), 1);
    cart.remove(0);
    expect(cart.lines().length).toBe(1);
    cart.clear();
    expect(cart.lines()).toEqual([]);
  });

  it('persists to localStorage and reloads into a fresh instance', () => {
    const cart = new CartService();
    cart.add(product({ priceCents: 500 }), 4);
    expect(localStorage.getItem('corporate-gifting-cart-v2')).toContain('"quantity":4');

    const reloaded = new CartService();
    expect(reloaded.count()).toBe(4);
    expect(reloaded.subtotalCents()).toBe(2000);
  });
});
