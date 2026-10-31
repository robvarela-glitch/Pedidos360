import { OrderStatus } from '../../core/models/order.model';

/** Etapas visibles para el cliente, en el orden en que avanza un pedido. */
export const TRACKING_STEPS: { status: OrderStatus; label: string; help: string }[] = [
  { status: 'CREADO', label: 'Recibido', help: 'Registramos tu pedido.' },
  { status: 'EN_PREPARACION', label: 'En preparación', help: 'Estamos preparando tus productos.' },
  { status: 'DESPACHADO', label: 'Despachado', help: 'Tu pedido va en camino.' },
  { status: 'ENTREGADO', label: 'Entregado', help: 'Recibiste tu pedido.' }
];

/** Índice de la etapa actual (-1 si el pedido está cancelado). */
export function stepIndex(status: OrderStatus): number {
  return TRACKING_STEPS.findIndex((s) => s.status === status);
}
