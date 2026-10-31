import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { PRODUCT_CATEGORIES, Product } from '../../core/models/product.model';
import { CatalogService } from '../../core/services/catalog.service';
import { describeHttpError } from '../../core/services/auth.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

/** Edición de producto (solo Administrador). */
@Component({
  selector: 'app-edit-product',
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    Sidebar
  ],
  templateUrl: './edit-product.html',
  styleUrl: './edit-product.css'
})
export class EditProduct implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly catalogService = inject(CatalogService);
  private readonly cdr = inject(ChangeDetectorRef);

  product: Product | undefined;
  loading = true;
  saving = false;
  saved = false;
  errorMessage = '';
  categories = PRODUCT_CATEGORIES;

  ngOnInit(): void {
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
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  saveProduct(form: NgForm): void {
    if (!this.product || form.invalid) {
      form.control.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.errorMessage = '';
    this.catalogService.updateProduct(this.product.id, this.product).subscribe({
      next: () => {
        this.saving = false;
        this.saved = true;
        this.cdr.markForCheck();
        setTimeout(() => this.router.navigate(['/catalog', this.product?.id]), 700);
      },
      error: (error) => {
        this.errorMessage = describeHttpError(error);
        this.saving = false;
        this.cdr.markForCheck();
      }
    });
  }

  cancel(): void {
    if (this.product) {
      this.router.navigate(['/catalog', this.product.id]);
      return;
    }
    this.router.navigate(['/catalog']);
  }
}
