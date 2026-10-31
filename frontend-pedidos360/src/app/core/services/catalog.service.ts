import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { apiUrl } from '../config/app-config';
import { Product } from '../models/product.model';

/** Respuesta JSON de ms-pedidos360-catalog. */
interface ProductResponse {
  id: number;
  sku: string;
  name: string;
  description: string | null;
  brand: string | null;
  category: string | null;
  imageUrl: string | null;
  price: number;
  stock: number;
  active: boolean;
  specifications: { type: string | null; connectivity: string | null; color: string | null; warranty: string | null } | null;
}

/** Catálogo real (BFF -> ms-catalog -> Oracle). Crear/editar/desactivar: solo Admin. */
@Injectable({ providedIn: 'root' })
export class CatalogService {

  private readonly http = inject(HttpClient);

  getProducts(includeInactive = false): Observable<Product[]> {
    const params = new HttpParams().set('includeInactive', includeInactive);
    return this.http.get<ProductResponse[]>(apiUrl('/api/catalog/products'), { params })
      .pipe(map((list) => list.map(toProduct)));
  }

  getProductById(id: number): Observable<Product> {
    return this.http.get<ProductResponse>(apiUrl('/api/catalog/products/' + id)).pipe(map(toProduct));
  }

  createProduct(product: Product): Observable<Product> {
    return this.http.post<ProductResponse>(apiUrl('/api/catalog/products'), toRequest(product)).pipe(map(toProduct));
  }

  updateProduct(id: number, product: Product): Observable<Product> {
    return this.http.put<ProductResponse>(apiUrl('/api/catalog/products/' + id), toRequest(product)).pipe(map(toProduct));
  }

  deactivateProduct(id: number): Observable<void> {
    return this.http.delete<void>(apiUrl('/api/catalog/products/' + id));
  }
}

function toProduct(p: ProductResponse): Product {
  return {
    id: p.id,
    sku: p.sku,
    name: p.name,
    description: p.description ?? '',
    brand: p.brand ?? '',
    category: p.category ?? '',
    image: p.imageUrl ?? '',
    price: p.price ?? 0,
    stock: p.stock ?? 0,
    active: p.active ?? true,
    specifications: {
      type: p.specifications?.type ?? '',
      connectivity: p.specifications?.connectivity ?? '',
      color: p.specifications?.color ?? '',
      warranty: p.specifications?.warranty ?? ''
    }
  };
}

function toRequest(p: Product) {
  return {
    sku: p.sku,
    name: p.name,
    description: p.description,
    brand: p.brand,
    category: p.category,
    imageUrl: p.image,
    price: Number(p.price),
    stock: Number(p.stock),
    specifications: { ...p.specifications }
  };
}
