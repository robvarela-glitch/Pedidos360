package cl.duoc.pedidos360.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "El SKU es obligatorio")
    @Size(max = 40)
    private String sku;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120)
    private String name;

    @Size(max = 1000)
    private String description;

    @Size(max = 80)
    private String brand;

    @Size(max = 80)
    private String category;

    @Size(max = 300)
    private String imageUrl;

    @NotNull(message = "El precio es obligatorio")
    @PositiveOrZero
    private Double price;

    @NotNull(message = "El stock es obligatorio")
    @PositiveOrZero
    private Integer stock;

    /** Especificaciones técnicas (opcionales). */
    @Valid
    private Specifications specifications;

    @Getter
    @Setter
    public static class Specifications {
        @Size(max = 120)
        private String type;
        @Size(max = 120)
        private String connectivity;
        @Size(max = 80)
        private String color;
        @Size(max = 80)
        private String warranty;
    }
}
