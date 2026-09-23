package cl.duoc.pedidos360.bff.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Enumeration;
import java.util.Map;

/**
 * Único controlador del BFF: actúa como intermediario ("reverse proxy")
 * entre el frontend Angular y los microservicios internos.
 *
 * Spring Security ya validó el JWT (firma, issuer, audience, expiración) y
 * autorizó el rol requerido para esta ruta (ver SecurityConfig) ANTES de que
 * la petición llegue aquí. Este controlador solo reenvía la petición —
 * mismo método, mismo path, mismos query params, mismo body — hacia el
 * microservicio real, agregando el mismo header Authorization, de modo que
 * ese microservicio también pueda validar el token (defensa en profundidad).
 *
 * Ejemplos de mapeo:
 *   GET  /api/orders          -> http://ms-orders:8081/api/orders
 *   POST /api/catalog/products -> http://ms-catalog:8082/api/catalog/products
 *   GET  /api/reports/summary -> http://ms-report:8084/api/reports/summary
 */
@RestController
public class ProxyController {

    private final RestClient restClient;
    private final Map<String, String> serviceBaseUrls;

    public ProxyController(RestClient.Builder restClientBuilder, Map<String, String> serviceBaseUrls) {
        this.restClient = restClientBuilder.build();
        this.serviceBaseUrls = serviceBaseUrls;
    }

    @RequestMapping("/api/{service}/**")
    public ResponseEntity<byte[]> proxy(jakarta.servlet.http.HttpServletRequest request,
                                         @org.springframework.web.bind.annotation.PathVariable String service) {

        String baseUrl = serviceBaseUrls.get(service);
        if (baseUrl == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio '" + service + "' no reconocido por el BFF");
        }

        String targetUrl = baseUrl + request.getRequestURI()
                + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

        byte[] body = readBody(request);

        return restClient.method(HttpMethod.valueOf(request.getMethod()))
                .uri(targetUrl)
                .headers(headers -> copyForwardableHeaders(request, headers))
                .body(body != null && body.length > 0 ? body : null)
                .exchange((req, resp) -> {
                    byte[] responseBody = resp.getBody() != null ? resp.getBody().readAllBytes() : new byte[0];
                    HttpHeaders responseHeaders = new HttpHeaders();
                    resp.getHeaders().forEach((name, values) -> {
                        // Content-Length/Transfer-Encoding los recalcula Spring al responder.
                        if (!name.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)
                                && !name.equalsIgnoreCase(HttpHeaders.TRANSFER_ENCODING)) {
                            responseHeaders.addAll(name, values);
                        }
                    });
                    return ResponseEntity.status(resp.getStatusCode()).headers(responseHeaders).body(responseBody);
                });
    }

    /** Reenvía Authorization (el JWT) y Content-Type; el resto de headers de infraestructura no se propagan. */
    private void copyForwardableHeaders(HttpServletRequest request, HttpHeaders headers) {
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.equalsIgnoreCase(HttpHeaders.AUTHORIZATION) || name.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
                headers.addAll(name, java.util.Collections.list(request.getHeaders(name)));
            }
        }
    }

    private byte[] readBody(HttpServletRequest request) {
        try {
            return request.getInputStream().readAllBytes();
        } catch (Exception e) {
            return null;
        }
    }
}
