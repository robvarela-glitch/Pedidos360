package cl.duoc.pedidos360.bff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "AZURE_TENANT_ID=test-tenant",
        "services.orders.url=http://localhost:8081",
        "services.catalog.url=http://localhost:8082",
        "services.notify.url=http://localhost:8083",
        "services.report.url=http://localhost:8084",
        "services.audit.url=http://localhost:8085",
        "services.rabbitadmin.url=http://localhost:8086"
})
class BffApplicationTests {

    // Reemplaza el JwtDecoder real: el test no necesita descargar las claves del tenant.
    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
        // Si el contexto de Spring (seguridad + enrutamiento) no levanta, este test falla.
    }
}
