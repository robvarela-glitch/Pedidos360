export interface Product {
  id: number;
  name: string;
  brand: string;
  sku: string;
  category: string;
  stock: number;
  price: number;
  /** Ruta de la imagen (en el backend: imageUrl). */
  image: string;
  description: string;
  active: boolean;
  specifications: ProductSpecifications;
}

export interface ProductSpecifications {
  type: string;
  connectivity: string;
  color: string;
  warranty: string;
}

export const PRODUCT_CATEGORIES = [
  'Notebooks',
  'Monitores',
  'Smartphones',
  'Teclados',
  'Mouse',
  'Audio'
];

export function emptyProduct(): Product {
  return {
    id: 0,
    name: '',
    brand: '',
    sku: '',
    category: '',
    stock: 0,
    price: 0,
    image: '',
    description: '',
    active: true,
    specifications: { type: '', connectivity: '', color: '', warranty: '' }
  };
}
