package cl.duoc.pedidos360.orders.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Datos del producto que Orders necesita del catálogo (precio oficial, stock y estado). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogProduct(Long id, String sku, String name, Double price, Integer stock, Boolean active) {
}
