import { Component } from '@angular/core';
import {
  FormsModule,
  NgForm
} from '@angular/forms';

import {
  Router,
  RouterLink
} from '@angular/router';

import { Product } from '../../core/models/product.model';
import { CatalogService } from '../../core/services/catalog.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

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

  categories = [
    'Notebooks',
    'Monitores',
    'Smartphones',
    'Teclados',
    'Mouse',
    'Audio'
  ];


  product: Product = {

    id: 0,

    name: '',

    brand: '',

    sku: '',

    category: '',

    stock: 0,

    price: 0,

    image: '',

    description: '',

    specifications: {

      type: '',

      connectivity: '',

      color: '',

      warranty: ''

    }

  };


  saving = false;

  saved = false;


  constructor(
    private catalogService: CatalogService,
    private router: Router
  ) {}


  createProduct(form: NgForm): void {

    if (form.invalid) {

      form.control.markAllAsTouched();

      return;

    }


    this.saving = true;


    const newProduct: Product = {

      ...this.product,

      id: Date.now()

    };


    this.catalogService
      .createProduct(newProduct)
      .subscribe({

        next: () => {

          this.saving = false;

          this.saved = true;


          setTimeout(() => {

            this.router.navigate([
              '/catalog'
            ]);

          }, 700);

        },


        error: (error) => {

          console.error(
            'Error al crear el producto:',
            error
          );

          this.saving = false;

        }

      });

  }


  cancel(): void {

    this.router.navigate([
      '/catalog'
    ]);

  }

}