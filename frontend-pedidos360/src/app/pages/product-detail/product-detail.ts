import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Product } from '../../core/models/product.model';
import { CatalogService } from '../../core/services/catalog.service';
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

  product: Product | undefined;

  loading = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private catalogService: CatalogService
  ) {}

  ngOnInit(): void {

    const id =
      Number(this.route.snapshot.paramMap.get('id'));

    this.loadProduct(id);
  }

  loadProduct(id: number): void {

    this.catalogService.getProducts().subscribe({
      next: (products) => {

        this.product =
          products.find(
            (product) => product.id === id
          );

        this.loading = false;

      },

      error: (error) => {

        console.error(
          'Error al cargar el producto:',
          error
        );

        this.loading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/catalog']);
  }

}