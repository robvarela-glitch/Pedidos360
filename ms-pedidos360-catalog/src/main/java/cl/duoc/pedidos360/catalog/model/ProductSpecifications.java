package cl.duoc.pedidos360.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Especificaciones técnicas del producto (columnas SPEC_* de PED360_PRODUCTS). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSpecifications {

    @Column(name = "SPEC_TYPE", length = 120)
    private String type;

    @Column(name = "SPEC_CONNECTIVITY", length = 120)
    private String connectivity;

    @Column(name = "SPEC_COLOR", length = 80)
    private String color;

    @Column(name = "SPEC_WARRANTY", length = 80)
    private String warranty;
}
