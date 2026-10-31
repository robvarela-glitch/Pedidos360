package cl.duoc.pedidos360.catalog.controller;

import cl.duoc.pedidos360.catalog.dto.ProductRequest;
import cl.duoc.pedidos360.catalog.model.Product;
import cl.duoc.pedidos360.catalog.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catálogo de productos.
 *   GET            -> cualquier usuario autenticado con rol (Admin, Operador, Cliente)
 *   POST/PUT/DELETE -> solo Admin
 */
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/publico/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ms-pedidos360-catalog activo");
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('Admin','Operador','Cliente')")
    public ResponseEntity<List<Product>> findAll(@RequestParam(defaultValue = "false") boolean includeInactive,
                                                 JwtAuthenticationToken auth) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_Admin"));
        return ResponseEntity.ok(productService.findAll(includeInactive && isAdmin));
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('Admin','Operador','Cliente')")
    public ResponseEntity<Product> findById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request, JwtAuthenticationToken auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request, actor(auth)));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Product> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request,
                                          JwtAuthenticationToken auth) {
        return ResponseEntity.ok(productService.update(id, request, actor(auth)));
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id, JwtAuthenticationToken auth) {
        productService.deactivate(id, actor(auth));
        return ResponseEntity.noContent().build();
    }

    private static String actor(JwtAuthenticationToken auth) {
        String name = auth.getToken().getClaimAsString("name");
        return name != null ? name : auth.getName();
    }
}
