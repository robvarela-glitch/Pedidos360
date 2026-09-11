import { Component, OnInit } from '@angular/core';
import { DecimalPipe } from '@angular/common';

import { Product } from '../../core/models/product.model';
import { CatalogService } from '../../core/services/catalog.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

@Component({
  selector: 'app-catalog',
  imports: [DecimalPipe, Sidebar],
  templateUrl: './catalog.html',
  styleUrl: './catalog.css'
})
export class Catalog implements OnInit {

  products: Product[] = [];

  constructor(private catalogService: CatalogService) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.catalogService.getProducts().subscribe({
      next: (data) => {
        this.products = data;
      },
      error: (error) => {
        console.error('Error al cargar los productos:', error);
      }
    });
  }
}