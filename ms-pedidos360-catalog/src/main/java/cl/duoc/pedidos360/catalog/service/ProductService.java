package cl.duoc.pedidos360.catalog.service;

import cl.duoc.pedidos360.catalog.dto.ProductRequest;
import cl.duoc.pedidos360.catalog.exception.ProductNotFoundException;
import cl.duoc.pedidos360.catalog.model.Product;
import cl.duoc.pedidos360.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Product create(ProductRequest request) {
        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .active(true)
                .build();
        return productRepository.save(product);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product product = findById(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        return productRepository.save(product);
    }

    @Transactional
    public void deactivate(Long id) {
        Product product = findById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    public void decreaseStock(Long id, int quantity) {
        Product product = findById(id);
        product.setStock(Math.max(0, product.getStock() - quantity));
        productRepository.save(product);
    }
}
