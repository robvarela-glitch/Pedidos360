import { Injectable, computed, effect, signal } from '@angular/core';

import { Product } from '../models/product.model';

/** Línea del carrito. El precio es referencial: el backend recalcula con el precio vigente. */
export interface CartLine {
  productId: number;
  name: string;
  brand: string;
  category: string;
  image: string;
  price: number;
  stock: number;
  quantity: number;
}

const STORAGE_KEY = 'pedidos360.cart';

/**
 * Carrito del cliente. Vive en el navegador (no es un pedido hasta confirmarlo):
 * al confirmar se envía POST /api/orders y el backend valida stock y precios.
 */
@Injectable({ providedIn: 'root' })
export class CartService {

  readonly lines = signal<CartLine[]>(load());
  readonly count = computed(() => this.lines().reduce((n, l) => n + l.quantity, 0));
  readonly total = computed(() => this.lines().reduce((t, l) => t + l.price * l.quantity, 0));
  readonly isEmpty = computed(() => this.lines().length === 0);

  constructor() {
    effect(() => {
      const lines = this.lines();
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(lines));
      } catch {
        // Navegador sin almacenamiento disponible: el carrito sigue funcionando en memoria.
      }
    });
  }

  quantityOf(productId: number): number {
    return this.lines().find((l) => l.productId === productId)?.quantity ?? 0;
  }

  /** Agrega unidades respetando el stock informado por el catálogo. Devuelve un mensaje para el usuario. */
  add(product: Product, quantity = 1): { ok: boolean; message: string } {
    if (!product.active || product.stock <= 0) {
      return { ok: false, message: product.name + ' no tiene stock disponible.' };
    }
    const current = this.quantityOf(product.id);
    const wanted = current + quantity;
    if (wanted > product.stock) {
      return { ok: false, message: 'Solo quedan ' + product.stock + ' unidades de ' + product.name + '.' };
    }
    if (current > 0) {
      this.lines.update((lines) => lines.map((l) =>
        l.productId === product.id ? { ...l, quantity: wanted, price: product.price, stock: product.stock } : l));
    } else {
      this.lines.update((lines) => [...lines, {
        productId: product.id,
        name: product.name,
        brand: product.brand,
        category: product.category,
        image: product.image,
        price: product.price,
        stock: product.stock,
        quantity
      }]);
    }
    return { ok: true, message: product.name + ' agregado al carrito.' };
  }

  setQuantity(productId: number, quantity: number): void {
    const q = Math.floor(quantity);
    if (q <= 0) {
      this.remove(productId);
      return;
    }
    this.lines.update((lines) => lines.map((l) =>
      l.productId === productId ? { ...l, quantity: Math.min(q, l.stock) } : l));
  }

  /** Actualiza precio y stock con los datos vigentes del catálogo. */
  refresh(products: Product[]): void {
    this.lines.update((lines) => lines.map((l) => {
      const p = products.find((x) => x.id === l.productId);
      return p ? { ...l, price: p.price, stock: p.stock, image: p.image, name: p.name } : l;
    }));
  }

  remove(productId: number): void {
    this.lines.update((lines) => lines.filter((l) => l.productId !== productId));
  }

  clear(): void {
    this.lines.set([]);
  }
}

function load(): CartLine[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}
