package cl.duoc.pedidos360.rabbitadmin.controller;

import cl.duoc.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.CreateExchangeRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.CreateQueueRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.OperationResponse;
import cl.duoc.pedidos360.rabbitadmin.dto.PublishMessageRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.QueueInfoResponse;
import cl.duoc.pedidos360.rabbitadmin.dto.ResourceNames;
import cl.duoc.pedidos360.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST del administrador de RabbitMQ. Todas las operaciones requieren JWT con rol Admin.
 *
 *   GET    /api/rabbitadmin/queues                 lista colas (mensajes, consumidores, nodo)
 *   GET    /api/rabbitadmin/queues/{name}          estado de una cola
 *   POST   /api/rabbitadmin/queues                 crea una cola (y su DLQ por defecto)
 *   DELETE /api/rabbitadmin/queues/{name}          elimina una cola (?includeDlq=true elimina su DLQ)
 *   DELETE /api/rabbitadmin/queues/{name}/messages vacía una cola
 *   GET    /api/rabbitadmin/exchanges              lista exchanges
 *   POST   /api/rabbitadmin/exchanges              crea un exchange (DIRECT, TOPIC, FANOUT, HEADERS)
 *   DELETE /api/rabbitadmin/exchanges/{name}       elimina un exchange
 *   GET    /api/rabbitadmin/bindings               lista bindings
 *   POST   /api/rabbitadmin/bindings               crea un binding exchange -> cola
 *   DELETE /api/rabbitadmin/bindings               elimina un binding (mismo cuerpo que POST)
 *   GET    /api/rabbitadmin/nodes                  nodos del clúster RabbitMQ
 *   POST   /api/rabbitadmin/messages               publica un mensaje de prueba
 */
@Validated
@RestController
@RequestMapping("/api/rabbitadmin")
@RequiredArgsConstructor
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    @GetMapping("/publico/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ms-pedidos360-rabbitadmin activo");
    }

    // ---- Colas ----

    @GetMapping("/queues")
    @PreAuthorize("hasRole('Admin')")
    public List<Map<String, Object>> listQueues() {
        return rabbitAdminService.listQueues();
    }

    @GetMapping("/queues/{name}")
    @PreAuthorize("hasRole('Admin')")
    public QueueInfoResponse getQueue(@PathVariable @NotBlank @Size(max = 200) String name) {
        return rabbitAdminService.getQueue(name);
    }

    @PostMapping("/queues")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<QueueInfoResponse> createQueue(@Valid @RequestBody CreateQueueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminService.createQueue(request));
    }

    @DeleteMapping("/queues/{name}")
    @PreAuthorize("hasRole('Admin')")
    public OperationResponse deleteQueue(
            @PathVariable @Pattern(regexp = ResourceNames.NAME_REGEX, message = ResourceNames.NAME_MESSAGE) String name,
            @RequestParam(defaultValue = "true") boolean includeDlq) {
        return rabbitAdminService.deleteQueue(name, includeDlq);
    }

    @DeleteMapping("/queues/{name}/messages")
    @PreAuthorize("hasRole('Admin')")
    public OperationResponse purgeQueue(@PathVariable @NotBlank @Size(max = 200) String name) {
        return rabbitAdminService.purgeQueue(name);
    }

    // ---- Exchanges ----

    @GetMapping("/exchanges")
    @PreAuthorize("hasRole('Admin')")
    public List<Map<String, Object>> listExchanges() {
        return rabbitAdminService.listExchanges();
    }

    @PostMapping("/exchanges")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<OperationResponse> createExchange(@Valid @RequestBody CreateExchangeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminService.createExchange(request));
    }

    @DeleteMapping("/exchanges/{name}")
    @PreAuthorize("hasRole('Admin')")
    public OperationResponse deleteExchange(
            @PathVariable @Pattern(regexp = ResourceNames.NAME_REGEX, message = ResourceNames.NAME_MESSAGE) String name) {
        return rabbitAdminService.deleteExchange(name);
    }

    // ---- Bindings ----

    @GetMapping("/bindings")
    @PreAuthorize("hasRole('Admin')")
    public List<Map<String, Object>> listBindings() {
        return rabbitAdminService.listBindings();
    }

    @PostMapping("/bindings")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<OperationResponse> createBinding(@Valid @RequestBody BindingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminService.createBinding(request));
    }

    @DeleteMapping("/bindings")
    @PreAuthorize("hasRole('Admin')")
    public OperationResponse deleteBinding(@Valid @RequestBody BindingRequest request) {
        return rabbitAdminService.deleteBinding(request);
    }

    // ---- Clúster y pruebas ----

    @GetMapping("/nodes")
    @PreAuthorize("hasRole('Admin')")
    public List<Map<String, Object>> listNodes() {
        return rabbitAdminService.listNodes();
    }

    @PostMapping("/messages")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<OperationResponse> publish(@Valid @RequestBody PublishMessageRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(rabbitAdminService.publish(request));
    }
}
