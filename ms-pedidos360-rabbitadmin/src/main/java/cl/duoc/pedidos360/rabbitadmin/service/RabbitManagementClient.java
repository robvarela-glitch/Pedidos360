package cl.duoc.pedidos360.rabbitadmin.service;

import cl.duoc.pedidos360.rabbitadmin.config.RabbitAdminProperties;
import cl.duoc.pedidos360.rabbitadmin.exception.BrokerOperationException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente de la API HTTP de administración de RabbitMQ (puerto 15672).
 * AMQP no permite LISTAR recursos, por eso las consultas de listado y de nodos
 * del clúster usan esta API. Crear/eliminar se hace por AMQP (RabbitAdminService).
 */
@Component
public class RabbitManagementClient {

    private static final String VHOST = "%2F"; // vhost "/" codificado

    private final RestClient restClient;
    private final String baseUrl;

    public RabbitManagementClient(RabbitAdminProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(10_000);
        this.baseUrl = props.getManagement().getUrl().replaceAll("/+$", "");
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeaders(h -> h.setBasicAuth(props.getManagement().getUsername(), props.getManagement().getPassword()))
                .build();
    }

    public List<Map<String, Object>> listQueues() {
        return project(get("/api/queues/" + VHOST), "name", "type", "durable", "messages", "consumers", "node", "arguments");
    }

    public List<Map<String, Object>> listExchanges() {
        List<Map<String, Object>> all = project(get("/api/exchanges/" + VHOST), "name", "type", "durable", "auto_delete");
        // Se omite el exchange por defecto (nombre vacío) para que el listado sea legible
        return all.stream().filter(e -> !"".equals(e.get("name"))).toList();
    }

    public List<Map<String, Object>> listBindings() {
        List<Map<String, Object>> all = project(get("/api/bindings/" + VHOST),
                "source", "destination", "destination_type", "routing_key");
        return all.stream().filter(b -> !"".equals(b.get("source"))).toList();
    }

    /** Nodos del clúster y si están en ejecución (evidencia del clúster de 2 nodos). */
    public List<Map<String, Object>> listNodes() {
        return project(get("/api/nodes"), "name", "running", "type", "uptime", "mem_used");
    }

    private JsonNode get(String path) {
        try {
            JsonNode body = restClient.get().uri(URI.create(baseUrl + path)).retrieve().body(JsonNode.class);
            return body == null ? com.fasterxml.jackson.databind.node.MissingNode.getInstance() : body;
        } catch (RestClientException e) {
            throw new BrokerOperationException("No se pudo consultar la API de administración de RabbitMQ: " + e.getMessage(), e);
        }
    }

    private static List<Map<String, Object>> project(JsonNode array, String... fields) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return result;
        }
        for (JsonNode item : array) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String field : fields) {
                JsonNode value = item.get(field);
                if (value == null || value.isNull()) {
                    row.put(field, null);
                } else if (value.isNumber()) {
                    row.put(field, value.numberValue());
                } else if (value.isBoolean()) {
                    row.put(field, value.booleanValue());
                } else if (value.isTextual()) {
                    row.put(field, value.textValue());
                } else {
                    row.put(field, value);
                }
            }
            result.add(row);
        }
        return result;
    }
}
