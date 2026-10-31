/** Estados reales del backend (ms-pedidos360-orders, enum OrderStatus). */
export type OrderStatus =
  | 'CREADO'
  | 'EN_PREPARACION'
  | 'DESPACHADO'
  | 'ENTREGADO'
  | 'CANCELADO';

export const ORDER_STATUSES: OrderStatus[] = [
  'CREADO',
  'EN_PREPARACION',
  'DESPACHADO',
  'ENTREGADO',
  'CANCELADO'
];

/** Transiciones permitidas (mismas reglas que el backend). */
export const NEXT_STATUSES: Record<OrderStatus, OrderStatus[]> = {
  CREADO: ['EN_PREPARACION', 'CANCELADO'],
  EN_PREPARACION: ['DESPACHADO', 'CANCELADO'],
  DESPACHADO: ['ENTREGADO'],
  ENTREGADO: [],
  CANCELADO: []
};

export interface OrderItem {
  id: number;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
}

/** Pedido tal como lo muestra la interfaz (adaptado desde la respuesta del backend). */
export interface Order {
  id: number;
  customerId: string;
  /** Nombre del cliente (claim "name" del JWT de quien creó el pedido). */
  customer: string;
  /** Fecha de creación formateada para mostrar. */
  date: string;
  createdAt: string;
  updatedAt?: string;
  status: OrderStatus;
  total: number;
  items: OrderItem[];
}

/** Cuerpo de POST /api/orders: el cliente y los precios los define el backend. */
export interface CreateOrderRequest {
  items: { productId: number; quantity: number }[];
}

export function statusLabel(status: string): string {
  switch (status) {
    case 'CREADO': return 'Creado';
    case 'EN_PREPARACION': return 'En preparación';
    case 'DESPACHADO': return 'Despachado';
    case 'ENTREGADO': return 'Entregado';
    case 'CANCELADO': return 'Cancelado';
    default: return status;
  }
}

export function statusClass(status: string): string {
  switch (status) {
    case 'CREADO': return 'created';
    case 'EN_PREPARACION': return 'preparing';
    case 'DESPACHADO': return 'dispatched';
    case 'ENTREGADO': return 'delivered';
    case 'CANCELADO': return 'cancelled';
    default: return '';
  }
}

export function formatPrice(price: number): string {
  return new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency: 'CLP',
    maximumFractionDigits: 0
  }).format(price ?? 0);
}
