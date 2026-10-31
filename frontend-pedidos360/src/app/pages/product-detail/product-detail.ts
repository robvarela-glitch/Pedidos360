import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Product } from '../../core/models/product.model';
import { CatalogService } from '../../core/services/catalog.service';
import { AuthService, describeHttpError } from '../../core/services/auth.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

@Component({
  selector: 'app-product-detail',
  imports: [
    CommonModule,
    RouterLink,
    Sidebar
  ],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.css'
})
export class ProductDetail implements OnInit {

  readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly catalogService = inject(CatalogService);
  private readonly cdr = inject(ChangeDetectorRef);

  product: Product | undefined;
  loading = true;
  working = false;
  message = '';
  messageOk = false;

  ngOnInit(): void {
    this.auth.loadMe().subscribe(() => this.cdr.markForCheck());
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadProduct(id);
  }

  loadProduct(id: number): void {
    this.catalogService.getProductById(id).subscribe({
      next: (product) => {
        this.product = product;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.product = undefined;
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  /** Solo Admin (el backend responde 403 a cualquier otro rol). */
  deactivate(): void {
    if (!this.product) {
      return;
    }
    this.working = true;
    this.catalogService.deactivateProduct(this.product.id).subscribe({
      next: () => {
        this.working = false;
        this.messageOk = true;
        this.message = 'Producto desactivado: ya no aparece en el catálogo de los clientes.';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.working = false;
        this.messageOk = false;
        this.message = describeHttpError(err);
        this.cdr.markForCheck();
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/catalog']);
  }
}
