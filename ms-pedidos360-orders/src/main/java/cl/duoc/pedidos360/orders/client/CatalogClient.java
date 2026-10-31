package cl.duoc.pedidos360.orders.client;

import cl.duoc.pedidos360.orders.exception.CatalogUnavailableException;
import cl.duoc.pedidos360.orders.exception.InvalidOrderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Consulta el catálogo para obtener el precio OFICIAL de cada producto.
 * Así el total del pedido no depende de precios enviados por el cliente.
 * Reenvía el mismo JWT del usuario: ms-catalog también lo valida.
 */
@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(@Value("${services.catalog.url:http://localhost:8082}") String catalogUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(10_000);
        this.restClient = RestClient.builder().baseUrl(catalogUrl).requestFactory(factory).build();
    }

    public CatalogProduct getProduct(Long productId, String bearerToken) {
        try {
            CatalogProduct product = restClient.get()
                    .uri("/api/catalog/products/{id}", productId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .retrieve()
                    .body(CatalogProduct.class);
            if (product == null) {
                throw new InvalidOrderException("El producto " + productId + " no existe en el catálogo");
            }
            return product;
        } catch (HttpClientErrorException.NotFound e) {
            throw new InvalidOrderException("El producto " + productId + " no existe en el catálogo");
        } catch (HttpClientErrorException e) {
            throw new CatalogUnavailableException("El catálogo rechazó la consulta del producto " + productId
                    + " (HTTP " + e.getStatusCode().value() + ")");
        } catch (ResourceAccessException e) {
            throw new CatalogUnavailableException("El catálogo no está disponible");
        }
    }
}
