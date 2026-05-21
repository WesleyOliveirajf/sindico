package br.com.sindico.app.gasto;

import br.com.sindico.app.config.SecurityConfig;
import br.com.sindico.app.support.WebMvcSecurityTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GastoApiController.class)
@Import(SecurityConfig.class)
class GastoApiControllerTest extends WebMvcSecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GastoService gastoService;

    // -----------------------------------------------------------------------
    // GET /api/gastos
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser
    void should_return_list_when_gastos_exist() throws Exception {
        Gasto g = gasto(UUID.randomUUID(), "Agua e esgoto");
        when(gastoService.listar(null, null, null)).thenReturn(List.of(g));

        mockMvc.perform(get("/api/gastos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descricao").value("Agua e esgoto"))
                .andExpect(jsonPath("$[0].tipo").value("FIXO"))
                .andExpect(jsonPath("$[0].valor").value(350.00));
    }

    @Test
    @WithMockUser
    void should_return_empty_array_when_no_gastos() throws Exception {
        when(gastoService.listar(null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/gastos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser
    void should_pass_filter_params_to_service() throws Exception {
        when(gastoService.listar(5, 2025, GastoTipo.AGUA)).thenReturn(List.of());

        mockMvc.perform(get("/api/gastos").param("mes", "5").param("ano", "2025").param("tipo", "AGUA"))
                .andExpect(status().isOk());

        verify(gastoService).listar(5, 2025, GastoTipo.AGUA);
    }

    // -----------------------------------------------------------------------
    // POST /api/gastos
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser
    void should_create_gasto_and_return_201() throws Exception {
        GastoRequest req = new GastoRequest(
                "Porteiro", GastoTipo.SALARIOS,
                BigDecimal.valueOf(2500), LocalDate.of(2025, 5, 1), true, null);
        Gasto g = gasto(UUID.randomUUID(), "Porteiro");
        when(gastoService.criar(any())).thenReturn(g);

        mockMvc.perform(post("/api/gastos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value("Porteiro"));
    }

    @Test
    @WithMockUser
    void should_return_400_when_descricao_blank() throws Exception {
        String body = "{\"descricao\":\"\",\"tipo\":\"FIXO\",\"valor\":100,\"dataGasto\":\"2025-05-01\"}";

        mockMvc.perform(post("/api/gastos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser
    void should_return_400_when_valor_zero() throws Exception {
        String body = "{\"descricao\":\"Agua\",\"tipo\":\"FIXO\",\"valor\":0,\"dataGasto\":\"2025-05-01\"}";

        mockMvc.perform(post("/api/gastos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser
    void should_return_400_when_tipo_absent() throws Exception {
        String body = "{\"descricao\":\"Agua\",\"valor\":100,\"dataGasto\":\"2025-05-01\"}";

        mockMvc.perform(post("/api/gastos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    // -----------------------------------------------------------------------
    // PUT /api/gastos/{id}
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser
    void should_update_gasto_and_return_200() throws Exception {
        UUID id = UUID.randomUUID();
        GastoRequest req = new GastoRequest(
                "Gas encanado", GastoTipo.GAS,
                BigDecimal.valueOf(180), LocalDate.of(2025, 5, 10), true, null);
        Gasto g = gasto(id, "Gas encanado");
        when(gastoService.atualizar(eq(id), any())).thenReturn(g);

        mockMvc.perform(put("/api/gastos/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Gas encanado"));
    }

    @Test
    @WithMockUser
    void should_return_404_when_gasto_not_found_on_update() throws Exception {
        UUID id = UUID.randomUUID();
        GastoRequest req = new GastoRequest(
                "X", GastoTipo.OUTROS,
                BigDecimal.valueOf(10), LocalDate.now(), false, null);
        doThrow(new EntityNotFoundException("Gasto nao encontrado."))
                .when(gastoService).atualizar(eq(id), any());

        mockMvc.perform(put("/api/gastos/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Gasto nao encontrado."));
    }

    // -----------------------------------------------------------------------
    // DELETE /api/gastos/{id}
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser
    void should_delete_gasto_and_return_204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/gastos/{id}", id).with(csrf()))
                .andExpect(status().isNoContent());

        verify(gastoService).deletar(id);
    }

    @Test
    @WithMockUser
    void should_return_404_when_gasto_not_found_on_delete() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Gasto nao encontrado."))
                .when(gastoService).deletar(id);

        mockMvc.perform(delete("/api/gastos/{id}", id).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Gasto nao encontrado."));
    }

    // -----------------------------------------------------------------------
    // Autenticação
    // -----------------------------------------------------------------------

    @Test
    void should_return_401_when_not_authenticated() throws Exception {
        mockMvc.perform(get("/api/gastos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_401_when_posting_without_auth() throws Exception {
        String body = "{\"descricao\":\"Agua\",\"tipo\":\"FIXO\",\"valor\":100,\"dataGasto\":\"2025-05-01\"}";
        mockMvc.perform(post("/api/gastos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private Gasto gasto(@SuppressWarnings("unused") UUID id, String descricao) {
        Gasto g = new Gasto();
        g.setCondominioId(UUID.randomUUID());
        g.setCriadoPor(UUID.randomUUID());
        g.setDescricao(descricao);
        g.setTipo(GastoTipo.AGUA);
        g.setValor(BigDecimal.valueOf(350));
        g.setDataGasto(LocalDate.of(2025, 5, 1));
        g.setFixo(true);
        return g;
    }
}

