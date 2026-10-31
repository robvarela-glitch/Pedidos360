package cl.duoc.pedidos360.orders.controller;

import cl.duoc.pedidos360.orders.dto.CreateOrderRequest;
import cl.duoc.pedidos360.orders.model.Order;
import cl.duoc.pedidos360.orders.model.OrderStatus;
import cl.duoc.pedidos360.orders.security.CurrentUser;
import cl.duoc.pedidos360.orders.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de pedidos. El usuario se obtiene del JWT validado (CurrentUser), nunca del JSON.
 *
 *   POST   /api/orders                 Cliente/Operador/Admin  crea un pedido propio
 *   GET    /api/orders[?status=]       Cliente: sus pedidos.  Operador/Admin: todos
 *   GET    /api/orders/{id}            Cliente: solo si es suyo (si no, 403)
 *   PATCH  /api/orders/{id}/status     Operador/Admin: cambia estado según transiciones válidas
 *   DELETE /api/orders/{id}            Cancela (Cliente: solo suyos y en estado CREADO)
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/publico/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ms-pedidos360-orders activo");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin','Operador','Cliente')")
    public ResponseEntity<Order> create(@Valid @RequestBody CreateOrderRequest request) {
        Order created = orderService.createOrder(request, CurrentUser.fromSecurityContext());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin','Operador','Cliente')")
    public ResponseEntity<List<Order>> findAll(@RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.findVisibleOrders(CurrentUser.fromSecurityContext(), status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin','Operador','Cliente')")
    public ResponseEntity<Order> findById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findVisibleOrder(id, CurrentUser.fromSecurityContext()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('Admin','Operador')")
    public ResponseEntity<Order> updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateStatus(id, status, CurrentUser.fromSecurityContext()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin','Operador','Cliente')")
    public ResponseEntity<Order> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id, CurrentUser.fromSecurityContext()));
    }
}
