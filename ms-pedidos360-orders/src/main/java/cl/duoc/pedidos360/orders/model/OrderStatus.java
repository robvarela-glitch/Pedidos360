package cl.duoc.pedidos360.orders.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de un pedido y transiciones permitidas:
 *   CREADO -> EN_PREPARACION | CANCELADO
 *   EN_PREPARACION -> DESPACHADO | CANCELADO
 *   DESPACHADO -> ENTREGADO
 *   ENTREGADO y CANCELADO son estados finales.
 */
public enum OrderStatus {
    CREADO,
    EN_PREPARACION,
    DESPACHADO,
    ENTREGADO,
    CANCELADO;

    public Set<OrderStatus> nextAllowed() {
        return switch (this) {
            case CREADO -> EnumSet.of(EN_PREPARACION, CANCELADO);
            case EN_PREPARACION -> EnumSet.of(DESPACHADO, CANCELADO);
            case DESPACHADO -> EnumSet.of(ENTREGADO);
            case ENTREGADO, CANCELADO -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canChangeTo(OrderStatus next) {
        return nextAllowed().contains(next);
    }
}
