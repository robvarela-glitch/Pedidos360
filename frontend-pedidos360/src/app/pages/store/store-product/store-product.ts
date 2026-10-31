import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Product } from '../../../core/models/product.model';
import { formatPrice } from '../../../core/models/order.model';
import { CatalogService } from '../../../core/services/catalog.service';
import { CartService } from '../../../core/services/cart.service';
import { describeHttpError } from '../../../core/services/auth.service';
import { StoreToastService } from '../store-toast.service';
import { ProductCard } from '../product-card/product-card';

/** Ficha de producto: imagen, precio, stock, cantidad, especificaciones y relacionados. */
@Component({
  selector: 'app-store-product',
  imports: [RouterLink, ProductCard],
  templateUrl: './store-product.html',
  styleUrl: './store-product.css'
})
export class StoreProduct implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);
  private readonly toast = inject(StoreToastService);

  readonly product = signal<Product | null>(null);
  readonly related = signal<Product[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly quantity = signal(1);

  readonly inCart = computed(() => (this.product() ? this.cart.quantityOf(this.product()!.id) : 0));
  readonly maxQuantity = computed(() => Math.max(0, (this.product()?.stock ?? 0) - this.inCart()));

  readonly specs = computed(() => {
    const s = this.product()?.specifications;
    if (!s) {
      return [];
    }
    return [
      { label: 'Tipo', value: s.type },
      { label: 'Conectividad', value: s.connectivity },
      { label: 'Color', value: s.color },
      { label: 'Garantía', value: s.warranty }
    ].filter((row) => !!row.value);
  });

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => this.load(Number(params.get('id'))));
  }

  private load(id: number): void {
    this.loading.set(true);
    this.error.set('');
    this.quantity.set(1);
    this.catalog.getProductById(id).subscribe({
      next: (product) => {
        this.product.set(product);
        this.loading.set(false);
        this.catalog.getProducts().subscribe({
          next: (all) => this.related.set(all
            .filter((p) => p.category === product.category && p.id !== product.id)
            .slice(0, 4)),
          error: () => this.related.set([])
        });
      },
      error: (err) => {
        this.product.set(null);
        this.error.set(err?.status === 404 ? 'Este producto no existe o ya no está disponible.' : describeHttpError(err));
        this.loading.set(false);
      }
    });
  }

  decrease(): void {
    this.quantity.update((q) => Math.max(1, q - 1));
  }

  increase(): void {
    this.quantity.update((q) => Math.min(this.maxQuantity(), q + 1));
  }

  addToCart(goToCart = false): void {
    const product = this.product();
    if (!product) {
      return;
    }
    const result = this.cart.add(product, this.quantity());
    this.toast.show(result.message, result.ok);
    if (result.ok) {
      this.quantity.set(1);
      if (goToCart) {
        this.router.navigate(['/tienda/carrito']);
      }
    }
  }

  price(value: number): string {
    return formatPrice(value);
  }
}
