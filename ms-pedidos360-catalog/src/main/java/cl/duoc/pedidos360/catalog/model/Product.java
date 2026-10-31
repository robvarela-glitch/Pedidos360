package cl.duoc.pedidos360.catalog.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PED360_PRODUCTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SKU", nullable = false, unique = true)
    private String sku;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Column(name = "BRAND")
    private String brand;

    @Column(name = "CATEGORY")
    private String category;

    /** Ruta de la imagen servida por el frontend (ej. /products/logitech-g915.webp). */
    @Column(name = "IMAGE_URL")
    private String imageUrl;

    @Embedded
    private ProductSpecifications specifications;

    @Column(name = "PRICE", nullable = false)
    private Double price;

    @Column(name = "STOCK", nullable = false)
    private Integer stock;

    @Column(name = "ACTIVE", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
