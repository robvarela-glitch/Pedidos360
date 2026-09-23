package cl.duoc.pedidos360.bff.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** JWT inválido: firma incorrecta, issuer/audience que no coinciden, o token expirado. */
    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(OAuth2AuthenticationException ex) {
        String detalle = ex.getError() != null ? ex.getError().getDescription() : null;
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(body(detalle != null ? detalle : "El token JWT no es válido", HttpStatus.UNAUTHORIZED));
    }

    /** JWT válido, pero el usuario no tiene el rol requerido para el endpoint. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(body("No tiene el rol necesario para esta operación", HttpStatus.FORBIDDEN));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status).body(body(ex.getReason(), status));
    }

    private Map<String, Object> body(String message, HttpStatus status) {
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", LocalDateTime.now());
        map.put("status", status.value());
        map.put("error", status.getReasonPhrase());
        map.put("message", message);
        return map;
    }
}
