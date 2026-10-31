package cl.duoc.pedidos360.bff.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

/**
 * Intermediario ("reverse proxy") entre el frontend y los microservicios internos.
 *
 * Spring Security ya validó el JWT y autorizó método + ruta + rol (SecurityConfig)
 * ANTES de llegar aquí. Este controlador reenvía la petición —mismo método, ruta,
 * query params y cuerpo— al microservicio correspondiente, con el mismo header
 * Authorization para que ese microservicio también valide el token.
 *
 *   GET  /api/orders           -> http://ms-orders:8081/api/orders
 *   POST /api/catalog/products -> http://ms-catalog:8082/api/catalog/products
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
    public ResponseEntity<byte[]> proxy(HttpServletRequest request, @PathVariable String service) {

        String baseUrl = serviceBaseUrls.get(service);
        if (baseUrl == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio '" + service + "' no reconocido por el BFF");
        }

        // URI.create: la ruta y los query params ya vienen codificados desde el navegador;
        // así no se vuelven a codificar.
        URI target = URI.create(baseUrl + request.getRequestURI()
                + (request.getQueryString() != null ? "?" + request.getQueryString() : ""));

        RestClient.RequestBodySpec spec = restClient.method(HttpMethod.valueOf(request.getMethod()))
                .uri(target)
                .headers(headers -> copyForwardableHeaders(request, headers));

        // Solo se envía cuerpo cuando la petición trae uno (POST/PUT/PATCH con JSON).
        // Un GET no lleva cuerpo: pasar null aquí provocaba un error interno.
        byte[] body = readBody(request);
        if (body.length > 0) {
            spec = spec.body(body);
        }

        return spec.exchange((req, resp) -> {
            byte[] responseBody = resp.getBody().readAllBytes();
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
                headers.addAll(name, Collections.list(request.getHeaders(name)));
            }
        }
    }

    private byte[] readBody(HttpServletRequest request) {
        try {
            return request.getInputStream().readAllBytes();
        } catch (Exception e) {
            return new byte[0];
        }
    }
}