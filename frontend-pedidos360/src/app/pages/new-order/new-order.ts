import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { formatPrice } from '../../core/models/order.model';
import { Product } from '../../core/models/product.model';
import { OrdersService } from '../../core/services/orders.service';
import { CatalogService } from '../../core/services/catalog.service';
import { AuthService, describeHttpError } from '../../core/services/auth.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

interface DraftItem {
  product: Product;
  quantity: number;
}

/**
 * Nuevo pedido. El cliente NO se escribe en el formulario: el backend lo toma del JWT
 * (claim oid / name). Los precios se muestran como referencia; el total oficial lo
 * calcula ms-orders con los precios del catálogo.
 */
@Component({
  selector: 'app-new-order',
  imports: [
    FormsModule,
    RouterLink,
    Sidebar
  ],
  templateUrl: './new-order.html',
  styleUrl: './new-order.css'
})
export class NewOrder implements OnInit {

  readonly auth = inject(AuthService);
  private readonly ordersService = inject(OrdersService);
  private readonly catalogService = inject(CatalogService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  products: Product[] = [];
  items: DraftItem[] = [];
  selectedProductId = '';
  quantity = 1;

  loadingProducts = true;
  saving = false;
  saved = false;
  errorMessage = '';

  ngOnInit(): void {
    this.auth.loadMe().subscribe(() => this.cdr.markForCheck());
    this.catalogService.getProducts().subscribe({
      next: (products) => {
        this.products = products.filter((p) => p.active && p.stock > 0);
        this.loadingProducts = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.errorMessage = describeHttpError(err);
        this.loadingProducts = false;
        this.cdr.markForCheck();
      }
    });
  }

  get total(): number {
    return this.items.reduce((sum, item) => sum + item.product.price * item.quantity, 0);
  }

  get selectedProduct(): Product | undefined {
    return this.products.find((p) => String(p.id) === String(this.selectedProductId));
  }

  addProduct(): void {
    const product = this.selectedProduct;
    const qty = Math.floor(Number(this.quantity));
    if (!product || !qty || qty < 1) {
      return;
    }
    const existing = this.items.find((i) => i.product.id === product.id);
    const newQty = (existing?.quantity ?? 0) + qty;
    if (newQty > product.stock) {
      this.errorMessage = 'Stock insuficiente para ' + product.name + ' (disponible: ' + product.stock + ').';
      return;
    }
    this.errorMessage = '';
    if (existing) {
      existing.quantity = newQty;
    } else {
      this.items.push({ product, quantity: qty });
    }
    this.selectedProductId = '';
    this.quantity = 1;
  }

  removeItem(item: DraftItem): void {
    this.items = this.items.filter((i) => i !== item);
  }

  createOrder(): void {
    if (this.items.length === 0 || this.saving) {
      return;
    }
    this.saving = true;
    this.errorMessage = '';
    this.ordersService.createOrder({
      items: this.items.map((i) => ({ productId: i.product.id, quantity: i.quantity }))
    }).subscribe({
      next: (order) => {
        this.saving = false;
        this.saved = true;
        this.cdr.markForCheck();
        setTimeout(() => this.router.navigate(['/orders', order.id]), 800);
      },
      error: (error) => {
        this.errorMessage = describeHttpError(error);
        this.saving = false;
        this.cdr.markForCheck();
      }
    });
  }

  formatPrice(price: number): string {
    return formatPrice(price);
  }

  cancel(): void {
    this.router.navigate(['/orders']);
  }
}
