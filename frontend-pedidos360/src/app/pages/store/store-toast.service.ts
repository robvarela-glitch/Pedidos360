import { Injectable, signal } from '@angular/core';

/** Aviso breve de la tienda (por ejemplo, "Producto agregado al carrito"). */
@Injectable({ providedIn: 'root' })
export class StoreToastService {
  readonly message = signal<{ text: string; ok: boolean } | null>(null);
  private timer: ReturnType<typeof setTimeout> | null = null;

  show(text: string, ok = true): void {
    this.message.set({ text, ok });
    if (this.timer) {
      clearTimeout(this.timer);
    }
    this.timer = setTimeout(() => this.message.set(null), 3200);
  }
}
