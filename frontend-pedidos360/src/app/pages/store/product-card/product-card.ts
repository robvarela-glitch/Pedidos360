import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Product } from '../../../core/models/product.model';
import { formatPrice } from '../../../core/models/order.model';
import { CartService } from '../../../core/services/cart.service';
import { StoreToastService } from '../store-toast.service';

/** Tarjeta de producto de la tienda, en vista de cuadrícula o de lista. */
@Component({
  selector: 'app-product-card',
  imports: [RouterLink],
  templateUrl: './product-card.html',
  styleUrl: './product-card.css'
})
export class ProductCard {

  readonly product = input.required<Product>();
  readonly layout = input<'grid' | 'list'>('grid');

  private readonly cart = inject(CartService);
  private readonly toast = inject(StoreToastService);

  readonly inCart = computed(() => this.cart.quantityOf(this.product().id));
  readonly available = computed(() => this.product().stock - this.inCart());

  readonly stockLabel = computed(() => {
    const stock = this.product().stock;
    if (stock <= 0) {
      return 'Sin stock';
    }
    if (stock <= 5) {
      return stock === 1 ? 'Última unidad' : 'Últimas ' + stock + ' unidades';
    }
    return stock + ' disponibles';
  });

  readonly stockState = computed(() => {
    const stock = this.product().stock;
    return stock <= 0 ? 'out' : stock <= 5 ? 'low' : 'ok';
  });

  add(): void {
    const result = this.cart.add(this.product(), 1);
    this.toast.show(result.message, result.ok);
  }

  price(value: number): string {
    return formatPrice(value);
  }
}
