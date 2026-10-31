import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { PRODUCT_CATEGORIES } from '../../../core/models/product.model';
import { StoreToastService } from '../store-toast.service';

/**
 * Estructura de la tienda (rol Cliente): encabezado con buscador, categorías,
 * carrito y cuenta. Las páginas hijas se muestran en el <router-outlet>.
 */
@Component({
  selector: 'app-store-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './store-layout.html',
  styleUrl: './store-layout.css'
})
export class StoreLayout implements OnInit {

  readonly auth = inject(AuthService);
  readonly cart = inject(CartService);
  readonly toast = inject(StoreToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly categories = PRODUCT_CATEGORIES;
  readonly accountOpen = signal(false);
  search = '';

  ngOnInit(): void {
    this.auth.loadMe().subscribe();
    this.route.queryParamMap.subscribe((params) => (this.search = params.get('q') ?? ''));
  }

  get initials(): string {
    const name = this.auth.displayName() || 'U';
    return name.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]!.toUpperCase()).join('');
  }

  submitSearch(event: Event): void {
    event.preventDefault();
    const q = this.search.trim();
    this.router.navigate(['/tienda'], { queryParams: q ? { q } : {} });
  }

  onSearchInput(event: Event): void {
    this.search = (event.target as HTMLInputElement).value;
  }

  toggleAccount(): void {
    this.accountOpen.update((v) => !v);
  }

  logout(): void {
    this.cart.clear();
    this.auth.logout();
  }
}
