package cl.duoc.pedidos360.bff.controller;

import cl.duoc.pedidos360.bff.config.RoutingConfig;
import cl.duoc.pedidos360.bff.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica la protección de endpoints por rol configurada en SecurityConfig,
 * SIN depender de una conexión real a los microservicios internos ni al
 * IDaaS: como el rechazo por autenticación/autorización ocurre en el filtro
 * de seguridad, la petición nunca llega a intentar la llamada HTTP saliente.
 *
 * Se importa SecurityConfig explícitamente porque @WebMvcTest, por defecto,
 * no garantiza incluir clases @Configuration de usuario fuera de las de
 * MVC/seguridad auto-detectadas; así el test usa las reglas reales, no un
 * contexto sin restricciones.
 */
@WebMvcTest(ProxyController.class)
@Import({RoutingConfig.class, SecurityConfig.class})
class ProxyControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // Evita que Spring intente resolver un issuer real al construir el contexto de test.
    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ordersLectura_conRolInsuficiente_retorna403() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(jwt().authorities(() -> "ROLE_Catalog.Read")))
                .andExpect(status().isForbidden());
    }

    @Test
    void ordersEscritura_conSoloRolLectura_retorna403() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(jwt().authorities(() -> "ROLE_Orders.Read")))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditoria_requiereRolEspecifico_retorna403SinRolAudit() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                        .with(jwt().authorities(() -> "ROLE_Orders.Read")))
                .andExpect(status().isForbidden());
    }

    @Test
    void servicioDesconocido_conRolAdmin_retorna404() throws Exception {
        // Admin pasa la autorización, pero "facturacion" no está mapeado a ningún microservicio.
        mockMvc.perform(get("/api/facturacion/algo")
                        .with(jwt().authorities(() -> "ROLE_Admin")))
                .andExpect(status().isNotFound());
    }
}
