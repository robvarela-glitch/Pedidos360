import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { Product } from '../models/product.model';

@Injectable({
  providedIn: 'root'
})
export class CatalogService {

  private apiUrl = '/api/catalog';

  private products: Product[] = [

    {
      id: 1,
      name: 'Logitech G915',
      brand: 'Logitech',
      sku: 'TEC-001',
      category: 'Teclados',
      stock: 12,
      price: 159990,
      image: '/products/logitech-g915.webp',
      description:
        'Teclado mecánico inalámbrico de alto rendimiento diseñado para gaming y productividad.',
      specifications: {
        type: 'Mecánico',
        connectivity: 'Bluetooth / Wireless',
        color: 'Negro',
        warranty: '2 años'
      }
    },

    {
      id: 2,
      name: 'Samsung Odyssey G5',
      brand: 'Samsung',
      sku: 'MON-002',
      category: 'Monitores',
      stock: 8,
      price: 279990,
      image: '/products/samsung-odyssey-g5.jpg',
      description:
        'Monitor gaming curvo diseñado para ofrecer una experiencia inmersiva y fluida.',
      specifications: {
        type: 'Gaming Curvo',
        connectivity: 'HDMI / DisplayPort',
        color: 'Negro',
        warranty: '1 año'
      }
    },

    {
      id: 3,
      name: 'ASUS TUF Gaming',
      brand: 'ASUS',
      sku: 'NOTE-003',
      category: 'Notebooks',
      stock: 5,
      price: 899990,
      image: '/products/asus-tuf-gaming.png',
      description:
        'Notebook gaming de alto rendimiento diseñado para videojuegos y tareas exigentes.',
      specifications: {
        type: 'Notebook Gaming',
        connectivity: 'Wi-Fi / Bluetooth',
        color: 'Negro',
        warranty: '1 año'
      }
    },

    {
      id: 4,
      name: 'Lenovo ThinkPad',
      brand: 'Lenovo',
      sku: 'NOTE-004',
      category: 'Notebooks',
      stock: 15,
      price: 699990,
      image: '/products/lenovo-thinkpad.avif',
      description:
        'Notebook empresarial orientado a productividad, movilidad y trabajo profesional.',
      specifications: {
        type: 'Notebook Empresarial',
        connectivity: 'Wi-Fi / Bluetooth',
        color: 'Negro',
        warranty: '1 año'
      }
    },

    {
      id: 5,
      name: 'Samsung Galaxy S26',
      brand: 'Samsung',
      sku: 'TEL-005',
      category: 'Smartphones',
      stock: 21,
      price: 999990,
      image: '/products/samsung-galaxy.jpg',
      description:
        'Smartphone Samsung de última generación con pantalla de alta calidad y gran rendimiento.',
      specifications: {
        type: 'Smartphone',
        connectivity: '5G / Wi-Fi / Bluetooth',
        color: 'Violeta',
        warranty: '1 año'
      }
    },

    {
      id: 6,
      name: 'HyperX Cloud III',
      brand: 'HyperX',
      sku: 'AUD-006',
      category: 'Audio',
      stock: 18,
      price: 89990,
      image: '/products/hyperx-cloud-iii.jpg',
      description:
        'Audífonos gaming diseñados para ofrecer sonido envolvente y comodidad durante largas sesiones.',
      specifications: {
        type: 'Audífonos Gaming',
        connectivity: 'USB / 3.5 mm',
        color: 'Negro / Rojo',
        warranty: '2 años'
      }
    },

    {
      id: 7,
      name: 'Logitech MX Master',
      brand: 'Logitech',
      sku: 'MOU-007',
      category: 'Mouse',
      stock: 14,
      price: 109990,
      image: '/products/logitech-mx-master.jpg',
      description:
        'Mouse inalámbrico premium diseñado para productividad y trabajo profesional.',
      specifications: {
        type: 'Mouse Inalámbrico',
        connectivity: 'Bluetooth / Wireless',
        color: 'Negro',
        warranty: '2 años'
      }
    }

  ];


  constructor(
    private http: HttpClient
  ) {}


  getProducts(): Observable<Product[]> {

    return of(this.products);

  }


  getProductById(
    id: number
  ): Observable<Product | undefined> {

    const product =
      this.products.find(
        (item) => item.id === id
      );

    return of(product);

  }


  createProduct(
    product: Product
  ): Observable<Product> {

    this.products.push(product);

    return of(product);

  }


  updateProduct(
    id: number,
    product: Product
  ): Observable<Product> {

    const index =
      this.products.findIndex(
        (item) => item.id === id
      );


    if (index !== -1) {

      this.products[index] = product;

    }


    return of(product);

  }


  updateProductLocal(
    product: Product
  ): Observable<Product> {

    return this.updateProduct(
      product.id,
      product
    );

  }

}