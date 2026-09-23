package cl.duoc.pedidos360.catalog.repository;

import cl.duoc.pedidos360.catalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
}
