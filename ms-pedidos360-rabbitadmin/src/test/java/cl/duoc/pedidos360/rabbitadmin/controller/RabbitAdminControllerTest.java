package cl.duoc.pedidos360.rabbitadmin.controller;

import cl.duoc.pedidos360.rabbitadmin.config.JwtSecuritySupport;
import cl.duoc.pedidos360.rabbitadmin.config.SecurityConfig;
import cl.duoc.pedidos360.rabbitadmin.dto.QueueInfoResponse;
import cl.duoc.pedidos360.rabbitadmin.exception.GlobalExceptionHandler;
import cl.duoc.pedidos360.rabbitadmin.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RabbitAdminController.class)
@Import({SecurityConfig.class, JwtSecuritySupport.class, GlobalExceptionHandler.class})
class RabbitAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private RabbitAdminService rabbitAdminService;
    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void sinToken_401() throws Exception {
        mockMvc.perform(get("/api/rabbitadmin/queues")).andExpect(status().isUnauthorized());
    }

    @Test
    void cliente_noPuedeAdministrar_403() throws Exception {
        mockMvc.perform(post("/api/rabbitadmin/queues").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"pruebas.cola\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Cliente"))))
                .andExpect(status().isForbidden());
        verify(rabbitAdminService, never()).createQueue(any());
    }

    @Test
    void admin_creaCola_201() throws Exception {
        when(rabbitAdminService.createQueue(any())).thenReturn(new QueueInfoResponse("pruebas.cola", 0, 0, "pruebas.cola.dlq"));

        mockMvc.perform(post("/api/rabbitadmin/queues").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"pruebas.cola\",\"withDeadLetterQueue\":true}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deadLetterQueue").value("pruebas.cola.dlq"));
    }

    @Test
    void admin_colaConNombreVacio_400() throws Exception {
        mockMvc.perform(post("/api/rabbitadmin/queues").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parámetros inválidos"));
        verify(rabbitAdminService, never()).createQueue(any());
    }

    @Test
    void admin_exchangeConTipoInvalido_400() throws Exception {
        mockMvc.perform(post("/api/rabbitadmin/exchanges").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"pruebas.x\",\"type\":\"CUALQUIERA\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isBadRequest());
    }
}
