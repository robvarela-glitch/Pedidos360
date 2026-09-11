import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule,
  NgForm
} from '@angular/forms';

import {
  ActivatedRoute,
  Router,
  RouterLink
} from '@angular/router';

import { Product } from '../../core/models/product.model';
import { CatalogService } from '../../core/services/catalog.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

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

  product: Product | undefined;

  loading = true;

  saving = false;

  saved = false;

  categories = [
    'Notebooks',
    'Monitores',
    'Smartphones',
    'Teclados',
    'Mouse',
    'Audio'
  ];


  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private catalogService: CatalogService
  ) {}


  ngOnInit(): void {

    const id =
      Number(
        this.route.snapshot.paramMap.get('id')
      );

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


  saveProduct(form: NgForm): void {

    if (!this.product || form.invalid) {

      form.control.markAllAsTouched();

      return;

    }


    this.saving = true;


    this.catalogService
      .updateProductLocal(this.product)
      .subscribe({

        next: () => {

          this.saving = false;

          this.saved = true;


          setTimeout(() => {

            this.router.navigate([
              '/catalog',
              this.product?.id
            ]);

          }, 700);

        },

        error: (error) => {

          console.error(
            'Error al guardar el producto:',
            error
          );

          this.saving = false;

        }

      });

  }


  cancel(): void {

    if (this.product) {

      this.router.navigate([
        '/catalog',
        this.product.id
      ]);

      return;

    }

    this.router.navigate([
      '/catalog'
    ]);

  }

}