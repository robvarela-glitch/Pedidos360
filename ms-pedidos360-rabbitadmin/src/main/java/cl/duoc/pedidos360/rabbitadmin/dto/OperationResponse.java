package cl.duoc.pedidos360.rabbitadmin.dto;

import java.time.LocalDateTime;

/** Respuesta estándar de las operaciones de administración. */
public record OperationResponse(String resource, String name, String action, String detail, LocalDateTime at) {

    public static OperationResponse of(String resource, String name, String action, String detail) {
        return new OperationResponse(resource, name, action, detail, LocalDateTime.now());
    }
}
