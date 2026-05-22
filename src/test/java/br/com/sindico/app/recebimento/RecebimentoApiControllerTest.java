package br.com.sindico.app.recebimento;

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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RecebimentoApiController.class)
@Import(SecurityConfig.class)
class RecebimentoApiControllerTest extends WebMvcSecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecebimentoService recebimentoService;

    @Test
    @WithMockUser
    void should_return_list_when_recebimentos_exist() throws Exception {
        Recebimento recebimento = recebimento("Taxa condominial");
        when(recebimentoService.listar(null, null, null)).thenReturn(List.of(recebimento));

        mockMvc.perform(get("/api/recebimentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descricao").value("Taxa condominial"))
                .andExpect(jsonPath("$[0].tipo").value("TAXA_CONDOMINIO"))
                .andExpect(jsonPath("$[0].valor").value(980.00));
    }

    @Test
    @WithMockUser
    void should_pass_filter_params_to_service() throws Exception {
        when(recebimentoService.listar(5, 2026, RecebimentoTipo.MULTA)).thenReturn(List.of());

        mockMvc.perform(get("/api/recebimentos").param("mes", "5").param("ano", "2026").param("tipo", "MULTA"))
                .andExpect(status().isOk());

        verify(recebimentoService).listar(5, 2026, RecebimentoTipo.MULTA);
    }

    @Test
    @WithMockUser
    void should_create_recebimento_and_return_201() throws Exception {
        RecebimentoRequest req = new RecebimentoRequest(
                "Taxa condominial",
                RecebimentoTipo.TAXA_CONDOMINIO,
                BigDecimal.valueOf(980),
                LocalDate.of(2026, 5, 10),
                null);
        when(recebimentoService.criar(any())).thenReturn(recebimento("Taxa condominial"));

        mockMvc.perform(post("/api/recebimentos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value("Taxa condominial"));
    }

    @Test
    @WithMockUser
    void should_return_400_when_descricao_blank() throws Exception {
        String body = "{\"descricao\":\"\",\"tipo\":\"TAXA_CONDOMINIO\",\"valor\":100,\"dataRecebimento\":\"2026-05-10\"}";

        mockMvc.perform(post("/api/recebimentos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser
    void should_return_400_when_valor_zero() throws Exception {
        String body = "{\"descricao\":\"Taxa\",\"tipo\":\"TAXA_CONDOMINIO\",\"valor\":0,\"dataRecebimento\":\"2026-05-10\"}";

        mockMvc.perform(post("/api/recebimentos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser
    void should_delete_recebimento_and_return_204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/recebimentos/{id}", id).with(csrf()))
                .andExpect(status().isNoContent());

        verify(recebimentoService).deletar(id);
    }

    @Test
    @WithMockUser
    void should_return_404_when_recebimento_not_found_on_delete() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Recebimento nao encontrado."))
                .when(recebimentoService).deletar(id);

        mockMvc.perform(delete("/api/recebimentos/{id}", id).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Recebimento nao encontrado."));
    }

    @Test
    void should_return_401_when_not_authenticated() throws Exception {
        mockMvc.perform(get("/api/recebimentos"))
                .andExpect(status().isUnauthorized());
    }

    private Recebimento recebimento(String descricao) {
        Recebimento recebimento = new Recebimento();
        recebimento.setCondominioId(UUID.randomUUID());
        recebimento.setCriadoPor(UUID.randomUUID());
        recebimento.setDescricao(descricao);
        recebimento.setTipo(RecebimentoTipo.TAXA_CONDOMINIO);
        recebimento.setValor(BigDecimal.valueOf(980));
        recebimento.setDataRecebimento(LocalDate.of(2026, 5, 10));
        return recebimento;
    }
}
