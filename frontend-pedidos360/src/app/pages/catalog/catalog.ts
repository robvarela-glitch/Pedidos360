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
  filteredProducts: Product[] = [];

  searchText = '';
  selectedCategory = '';

  constructor(private catalogService: CatalogService) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.catalogService.getProducts().subscribe({
      next: (data) => {
        this.products = data;
        this.filteredProducts = data;
      },
      error: (error) => {
        console.error('Error al cargar los productos:', error);
      }
    });
  }

  filterProducts(): void {
    const search = this.searchText.toLowerCase().trim();

    this.filteredProducts = this.products.filter((product) => {

      const matchesSearch =
        product.name.toLowerCase().includes(search) ||
        product.sku.toLowerCase().includes(search);

      const matchesCategory =
        this.selectedCategory === '' ||
        product.category === this.selectedCategory;

      return matchesSearch && matchesCategory;
    });
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;

    this.searchText = input.value;

    this.filterProducts();
  }

  onCategoryChange(event: Event): void {
    const select = event.target as HTMLSelectElement;

    this.selectedCategory = select.value;

    this.filterProducts();
  }
}