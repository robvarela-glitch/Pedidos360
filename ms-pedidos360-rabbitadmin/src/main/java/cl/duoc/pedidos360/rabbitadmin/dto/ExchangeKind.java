package cl.duoc.pedidos360.rabbitadmin.dto;

/** Tipos de exchange permitidos (cualquier otro valor se rechaza con 400). */
public enum ExchangeKind {
    DIRECT, TOPIC, FANOUT, HEADERS;

    public String amqpType() {
        return name().toLowerCase();
    }
}
