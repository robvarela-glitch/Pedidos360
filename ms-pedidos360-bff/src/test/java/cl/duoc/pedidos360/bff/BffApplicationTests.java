package cl.duoc.pedidos360.bff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.microsoftonline.com/test-tenant/v2.0",
        "pedidos360.security.audience=api://ms-pedidos360-bff",
        "services.orders.url=http://localhost:8081",
        "services.catalog.url=http://localhost:8082",
        "services.notify.url=http://localhost:8083",
        "services.report.url=http://localhost:8084",
        "services.audit.url=http://localhost:8085"
})
class BffApplicationTests {

    // Reemplaza el JwtDecoder real: evita que el contexto de test intente
    // contactar a un tenant de Azure AD inexistente ("test-tenant") al levantar.
    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
        // Si el contexto de Spring (seguridad + enrutamiento) no levanta, este test falla.
    }
}
