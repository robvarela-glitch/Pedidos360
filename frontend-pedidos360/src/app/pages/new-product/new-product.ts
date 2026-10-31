import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { PRODUCT_CATEGORIES, Product, emptyProduct } from '../../core/models/product.model';
import { CatalogService } from '../../core/services/catalog.service';
import { describeHttpError } from '../../core/services/auth.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

/** Alta de producto (solo Administrador). Se guarda en Oracle mediante ms-catalog. */
@Component({
  selector: 'app-new-product',
  imports: [
    FormsModule,
    RouterLink,
    Sidebar
  ],
  templateUrl: './new-product.html',
  styleUrl: './new-product.css'
})
export class NewProduct {

  private readonly catalogService = inject(CatalogService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  categories = PRODUCT_CATEGORIES;
  product: Product = emptyProduct();
  saving = false;
  saved = false;
  errorMessage = '';

  createProduct(form: NgForm): void {
    if (form.invalid) {
      form.control.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.errorMessage = '';
    this.catalogService.createProduct(this.product).subscribe({
      next: (created) => {
        this.saving = false;
        this.saved = true;
        this.cdr.markForCheck();
        setTimeout(() => this.router.navigate(['/catalog', created.id]), 700);
      },
      error: (error) => {
        this.errorMessage = describeHttpError(error);
        this.saving = false;
        this.cdr.markForCheck();
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/catalog']);
  }
}
