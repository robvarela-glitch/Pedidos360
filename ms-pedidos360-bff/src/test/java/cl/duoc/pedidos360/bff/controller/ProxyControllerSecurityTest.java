package cl.duoc.pedidos360.bff.controller;

import cl.duoc.pedidos360.bff.config.JwtSecuritySupport;
import cl.duoc.pedidos360.bff.config.RoutingConfig;
import cl.duoc.pedidos360.bff.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica las reglas de SecurityConfig (método + ruta + rol) y CORS, sin llamar a
 * los microservicios: el rechazo ocurre en el filtro de seguridad, antes del proxy.
 */
@WebMvcTest(controllers = {ProxyController.class, MeController.class})
@Import({RoutingConfig.class, SecurityConfig.class, JwtSecuritySupport.class})
class ProxyControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // Evita descargar las claves reales del tenant al construir el contexto de test.
    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cliente_noPuedeCambiarEstadoDePedido_403() throws Exception {
        mockMvc.perform(patch("/api/orders/1/status").param("status", "DESPACHADO")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Cliente"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void operador_noPuedeVerReportes_403() throws Exception {
        mockMvc.perform(get("/api/reports/summary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Operador"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cliente_noPuedeCrearProductos_403() throws Exception {
        mockMvc.perform(post("/api/catalog/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Cliente"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cliente_noPuedeAdministrarRabbit_403() throws Exception {
        mockMvc.perform(get("/api/rabbitadmin/queues")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Cliente"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void metodoNoDeclarado_seRechazaAunqueSeaAdmin_403() throws Exception {
        mockMvc.perform(put("/api/orders/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void servicioDesconocido_seRechaza_403() throws Exception {
        mockMvc.perform(get("/api/facturacion/algo")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void me_devuelveIdentidadDelToken() throws Exception {
        mockMvc.perform(get("/api/me")
                        .with(jwt()
                                .jwt(j -> j.claim("oid", "11111111-aaaa").claim("name", "Ana")
                                        .claim("scp", "access_as_user").claim("roles", List.of("Cliente")))
                                .authorities(new SimpleGrantedAuthority("ROLE_Cliente"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("11111111-aaaa"))
                .andExpect(jsonPath("$.roles", hasItem("Cliente")));
    }

    @Test
    void cors_preflightDesdeOrigenPermitido_ok() throws Exception {
        mockMvc.perform(options("/api/orders")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void cors_preflightDesdeOrigenNoPermitido_403() throws Exception {
        mockMvc.perform(options("/api/orders")
                        .header("Origin", "https://sitio-malicioso.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
