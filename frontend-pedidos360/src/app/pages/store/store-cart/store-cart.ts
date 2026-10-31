import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { formatPrice } from '../../../core/models/order.model';
import { CartLine, CartService } from '../../../core/services/cart.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { OrdersService } from '../../../core/services/orders.service';
import { AuthService, describeHttpError } from '../../../core/services/auth.service';

/**
 * Carrito y confirmación de compra. Al confirmar se crea el pedido real con
 * POST /api/orders: el backend toma el cliente del token, valida stock y usa
 * el precio vigente del catálogo. El pedido queda guardado en Oracle.
 */
@Component({
  selector: 'app-store-cart',
  imports: [RouterLink],
  templateUrl: './store-cart.html',
  styleUrl: './store-cart.css'
})
export class StoreCart implements OnInit {

  readonly cart = inject(CartService);
  readonly auth = inject(AuthService);
  private readonly catalog = inject(CatalogService);
  private readonly orders = inject(OrdersService);
  private readonly router = inject(Router);

  readonly placing = signal(false);
  readonly error = signal('');
  readonly refreshed = signal(false);

  ngOnInit(): void {
    this.auth.loadMe().subscribe();
    // Actualiza precio y stock del carrito con los datos vigentes del catálogo
    this.catalog.getProducts().subscribe({
      next: (products) => {
        this.cart.refresh(products);
        this.refreshed.set(true);
      },
      error: () => this.refreshed.set(true)
    });
  }

  decrease(line: CartLine): void {
    this.cart.setQuantity(line.productId, line.quantity - 1);
  }

  increase(line: CartLine): void {
    this.cart.setQuantity(line.productId, line.quantity + 1);
  }

  remove(line: CartLine): void {
    this.cart.remove(line.productId);
  }

  hasStockProblem(): boolean {
    return this.cart.lines().some((l) => l.stock <= 0 || l.quantity > l.stock);
  }

  placeOrder(): void {
    if (this.cart.isEmpty() || this.placing() || this.hasStockProblem()) {
      return;
    }
    this.placing.set(true);
    this.error.set('');
    this.orders.createOrder({
      items: this.cart.lines().map((l) => ({ productId: l.productId, quantity: l.quantity }))
    }).subscribe({
      next: (order) => {
        this.cart.clear();
        this.placing.set(false);
        this.router.navigate(['/tienda/pedidos', order.id], { queryParams: { nuevo: 1 } });
      },
      error: (err) => {
        this.placing.set(false);
        this.error.set(describeHttpError(err));
      }
    });
  }

  price(value: number): string {
    return formatPrice(value);
  }
}
