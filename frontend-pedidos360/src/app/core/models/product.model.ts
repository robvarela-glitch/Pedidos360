export interface Product {
  id: number;
  name: string;
  brand: string;
  sku: string;
  category: string;
  stock: number;
  price: number;
  image: string;
  description: string;
  specifications: ProductSpecifications;
}

export interface ProductSpecifications {
  type: string;
  connectivity: string;
  color: string;
  warranty: string;
}