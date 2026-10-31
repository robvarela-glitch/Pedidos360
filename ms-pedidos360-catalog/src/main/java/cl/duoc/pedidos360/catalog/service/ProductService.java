package cl.duoc.pedidos360.catalog.service;

import cl.duoc.pedidos360.catalog.dto.ProductRequest;
import cl.duoc.pedidos360.catalog.exception.DuplicateSkuException;
import cl.duoc.pedidos360.catalog.exception.ProductNotFoundException;
import cl.duoc.pedidos360.catalog.messaging.ProductEventPublisher;
import cl.duoc.pedidos360.catalog.model.Product;
import cl.duoc.pedidos360.catalog.model.ProductSpecifications;
import cl.duoc.pedidos360.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductEventPublisher eventPublisher;

    @Transactional
    public Product create(ProductRequest request, String performedBy) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }
        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .brand(request.getBrand())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .price(request.getPrice())
                .stock(request.getStock())
                .specifications(toSpecifications(request))
                .active(true)
                .build();
        Product saved = productRepository.save(product);
        eventPublisher.publish(saved, ProductEventPublisher.PRODUCT_CREATED, performedBy);
        return saved;
    }

    /** Clientes y operadores ven solo productos activos; el administrador puede pedir todos. */
    @Transactional(readOnly = true)
    public List<Product> findAll(boolean includeInactive) {
        return includeInactive
                ? productRepository.findAllByOrderByNameAsc()
                : productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public Product update(Long id, ProductRequest request, String performedBy) {
        Product product = findById(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setSpecifications(toSpecifications(request));
        Product saved = productRepository.save(product);
        eventPublisher.publish(saved, ProductEventPublisher.PRODUCT_UPDATED, performedBy);
        return saved;
    }

    private static ProductSpecifications toSpecifications(ProductRequest request) {
        ProductRequest.Specifications s = request.getSpecifications();
        if (s == null) {
            return null;
        }
        return ProductSpecifications.builder()
                .type(s.getType()).connectivity(s.getConnectivity())
                .color(s.getColor()).warranty(s.getWarranty())
                .build();
    }

    @Transactional
    public void deactivate(Long id, String performedBy) {
        Product product = findById(id);
        product.setActive(false);
        Product saved = productRepository.save(product);
        eventPublisher.publish(saved, ProductEventPublisher.PRODUCT_DEACTIVATED, performedBy);
    }
}
