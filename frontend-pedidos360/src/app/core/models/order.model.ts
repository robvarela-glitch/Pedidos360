export interface Order {
  id: number;
  customer: string;
  date: string;
  status: OrderStatus;
  total: number;
}

export type OrderStatus =
  | 'CREADO'
  | 'ACEPTADO'
  | 'EN_PREPARACIÓN'
  | 'DESPACHADO'
  | 'ENTREGADO'
  | 'CANCELADO';