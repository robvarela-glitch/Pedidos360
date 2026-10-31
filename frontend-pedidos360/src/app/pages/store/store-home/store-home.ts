import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { PRODUCT_CATEGORIES, Product } from '../../../core/models/product.model';
import { Order, formatPrice, statusLabel } from '../../../core/models/order.model';
import { CatalogService } from '../../../core/services/catalog.service';
import { OrdersService } from '../../../core/services/orders.service';
import { CartService } from '../../../core/services/cart.service';
import { describeHttpError } from '../../../core/services/auth.service';
import { StoreToastService } from '../store-toast.service';
import { ProductCard } from '../product-card/product-card';

type SortOption = 'name' | 'price-asc' | 'price-desc' | 'stock';

/**
 * Portada de la tienda (rol Cliente): producto destacado, categorías y catálogo
 * con búsqueda, filtro por categoría, orden y vista de cuadrícula / lista.
 * Todos los productos vienen de ms-catalog (Oracle) a través del BFF.
 */
@Component({
  selector: 'app-store-home',
  imports: [RouterLink, ProductCard],
  templateUrl: './store-home.html',
  styleUrl: './store-home.css'
})
export class StoreHome implements OnInit {

  private readonly catalog = inject(CatalogService);
  private readonly ordersService = inject(OrdersService);
  private readonly cart = inject(CartService);
  private readonly toast = inject(StoreToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly products = signal<Product[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly lastOrder = signal<Order | null>(null);

  readonly query = signal('');
  readonly category = signal('');
  readonly sort = signal<SortOption>('name');
  readonly view = signal<'grid' | 'list'>(loadView());

  readonly categories = PRODUCT_CATEGORIES;

  /** Producto destacado: el de mayor valor con stock disponible. */
  readonly featured = computed(() =>
    [...this.products()].filter((p) => p.stock > 0).sort((a, b) => b.price - a.price)[0] ?? null);

  readonly categoryTiles = computed(() => this.categories
    .map((name) => {
      const items = this.products().filter((p) => p.category === name);
      return { name, count: items.length, image: items.find((p) => p.image)?.image ?? '' };
    })
    .filter((tile) => tile.count > 0));

  readonly filtered = computed(() => {
    const q = this.query().toLowerCase().trim();
    const cat = this.category();
    const list = this.products().filter((p) => {
      const matchesText = !q || [p.name, p.brand, p.category, p.sku, p.description]
        .some((field) => (field ?? '').toLowerCase().includes(q));
      return matchesText && (!cat || p.category === cat);
    });
    switch (this.sort()) {
      case 'price-asc': return [...list].sort((a, b) => a.price - b.price);
      case 'price-desc': return [...list].sort((a, b) => b.price - a.price);
      case 'stock': return [...list].sort((a, b) => b.stock - a.stock);
      default: return [...list].sort((a, b) => a.name.localeCompare(b.name, 'es'));
    }
  });

  readonly isFiltering = computed(() => !!this.query().trim() || !!this.category());

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      this.query.set(params.get('q') ?? '');
      this.category.set(params.get('cat') ?? '');
    });

    this.catalog.getProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        this.cart.refresh(products);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(describeHttpError(err));
        this.loading.set(false);
      }
    });

    this.ordersService.getOrders().subscribe({
      next: (orders) => this.lastOrder.set(orders.find((o) => o.status !== 'CANCELADO' && o.status !== 'ENTREGADO') ?? null),
      error: () => this.lastOrder.set(null)
    });
  }

  setCategory(name: string): void {
    this.router.navigate(['/tienda'], {
      queryParams: { cat: name || null, q: this.query() || null }
    });
  }

  clearFilters(): void {
    this.router.navigate(['/tienda']);
  }

  onSort(event: Event): void {
    this.sort.set((event.target as HTMLSelectElement).value as SortOption);
  }

  setView(view: 'grid' | 'list'): void {
    this.view.set(view);
    try {
      localStorage.setItem('pedidos360.view', view);
    } catch {
      // sin almacenamiento local
    }
  }

  addFeatured(): void {
    const product = this.featured();
    if (product) {
      const result = this.cart.add(product, 1);
      this.toast.show(result.message, result.ok);
    }
  }

  price(value: number): string {
    return formatPrice(value);
  }

  status(value: string): string {
    return statusLabel(value);
  }
}

function loadView(): 'grid' | 'list' {
  try {
    return localStorage.getItem('pedidos360.view') === 'list' ? 'list' : 'grid';
  } catch {
    return 'grid';
  }
}
