import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PRODUCT_CATEGORIES, Product } from '../../core/models/product.model';
import { formatPrice } from '../../core/models/order.model';
import { CatalogService } from '../../core/services/catalog.service';
import { AuthService, describeHttpError } from '../../core/services/auth.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

/** Catálogo desde Oracle (ms-catalog). Solo el Administrador puede crear o editar productos. */
@Component({
  selector: 'app-catalog',
  imports: [
    RouterLink,
    Sidebar
  ],
  templateUrl: './catalog.html',
  styleUrl: './catalog.css'
})
export class Catalog implements OnInit {

  readonly auth = inject(AuthService);
  private readonly catalogService = inject(CatalogService);
  private readonly cdr = inject(ChangeDetectorRef);

  products: Product[] = [];
  filteredProducts: Product[] = [];
  searchText = '';
  selectedCategory = '';
  categories = PRODUCT_CATEGORIES;
  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    this.auth.loadMe().subscribe(() => this.cdr.markForCheck());
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    this.errorMessage = '';
    this.catalogService.getProducts().subscribe({
      next: (data) => {
        this.products = data;
        this.filterProducts();
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.errorMessage = describeHttpError(error);
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  filterProducts(): void {
    const search = this.searchText.toLowerCase().trim();
    this.filteredProducts = this.products.filter((product) => {
      const matchesSearch =
        product.name.toLowerCase().includes(search) ||
        product.brand.toLowerCase().includes(search) ||
        product.sku.toLowerCase().includes(search);
      const matchesCategory = this.selectedCategory === '' || product.category === this.selectedCategory;
      return matchesSearch && matchesCategory;
    });
  }

  onSearch(event: Event): void {
    this.searchText = (event.target as HTMLInputElement).value;
    this.filterProducts();
  }

  onCategoryChange(event: Event): void {
    this.selectedCategory = (event.target as HTMLSelectElement).value;
    this.filterProducts();
  }

  getTotalStock(): number {
    return this.products.reduce((total, product) => total + product.stock, 0);
  }

  formatPrice(price: number): string {
    return formatPrice(price);
  }
}
