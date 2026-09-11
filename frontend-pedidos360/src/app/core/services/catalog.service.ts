import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { Product } from '../models/product.model';

@Injectable({
  providedIn: 'root'
})
export class CatalogService {

  private apiUrl = '/api/catalog';

  constructor(private http: HttpClient) {}

  getProducts(): Observable<Product[]> {

    const mockProducts: Product[] = [
      {
        id: 1,
        name: 'Notebook Lenovo',
        sku: 'SKU-001',
        category: 'Tecnología',
        stock: 15,
        price: 599990
      },
      {
        id: 2,
        name: 'Mouse Logitech',
        sku: 'SKU-002',
        category: 'Accesorios',
        stock: 42,
        price: 19990
      },
      {
        id: 3,
        name: 'Teclado Mecánico',
        sku: 'SKU-003',
        category: 'Accesorios',
        stock: 4,
        price: 39990
      },
      {
        id: 4,
        name: 'Smartphone Samsung',
        sku: 'SKU-004',
        category: 'Telefonía',
        stock: 21,
        price: 399990
      }
    ];

    return of(mockProducts);
  }

  getProductById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/${id}`);
  }

  createProduct(product: Product): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, product);
  }

  updateProduct(id: number, product: Product): Observable<Product> {
    return this.http.put<Product>(
      `${this.apiUrl}/${id}`,
      product
    );
  }
}