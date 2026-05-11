package br.com.sindico.app.manutencao;

import br.com.sindico.app.config.SecurityConfig;
import br.com.sindico.app.support.WebMvcSecurityTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
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

@WebMvcTest(controllers = ManutencaoApiController.class)
@Import(SecurityConfig.class)
class ManutencaoApiControllerTest extends WebMvcSecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ManutencaoService manutencaoService;

    @Test
    @WithMockUser
    void getListaManutencoes() throws Exception {
        Manutencao m = manutencao(UUID.randomUUID(), "Troca de bomba");
        when(manutencaoService.listarDoCondominioAtual()).thenReturn(List.of(m));

        mockMvc.perform(get("/api/manutencoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Troca de bomba"))
                .andExpect(jsonPath("$[0].tipo").value("PREVENTIVA"))
                .andExpect(jsonPath("$[0].status").value("ABERTA"));
    }

    @Test
    @WithMockUser
    void getListaVaziaRetornaArrayVazio() throws Exception {
        when(manutencaoService.listarDoCondominioAtual()).thenReturn(List.of());

        mockMvc.perform(get("/api/manutencoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser
    void postCriaManutencao() throws Exception {
        ManutencaoRequest req = new ManutencaoRequest(
                "Pintura fachada", null, ManutencaoTipo.CORRETIVA,
                null, null, null, null, null,
                null, null, null, null, ManutencaoStatus.ABERTA, null);
        Manutencao m = manutencao(UUID.randomUUID(), "Pintura fachada");
        m.setTipo(ManutencaoTipo.CORRETIVA);
        when(manutencaoService.criar(any())).thenReturn(m);

        mockMvc.perform(post("/api/manutencoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Pintura fachada"));
    }

    @Test
    @WithMockUser
    void postTituloEmRetorna400() throws Exception {
        String body = "{\"titulo\":\"\",\"tipo\":\"PREVENTIVA\",\"status\":\"ABERTA\"}";

        mockMvc.perform(post("/api/manutencoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser
    void postSemTipoRetorna400() throws Exception {
        String body = "{\"titulo\":\"Reparo\",\"status\":\"ABERTA\"}";

        mockMvc.perform(post("/api/manutencoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser
    void postSemStatusRetorna400() throws Exception {
        String body = "{\"titulo\":\"Reparo\",\"tipo\":\"PREVENTIVA\"}";

        mockMvc.perform(post("/api/manutencoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser
    void putAtualizaManutencao() throws Exception {
        UUID id = UUID.randomUUID();
        ManutencaoRequest req = new ManutencaoRequest(
                "Reparo elevador", null, ManutencaoTipo.CORRETIVA,
                null, null, null, null, null,
                null, null, null, null, ManutencaoStatus.EM_ANDAMENTO, null);
        Manutencao m = manutencao(id, "Reparo elevador");
        m.setStatus(ManutencaoStatus.EM_ANDAMENTO);
        when(manutencaoService.atualizar(eq(id), any())).thenReturn(m);

        mockMvc.perform(put("/api/manutencoes/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Reparo elevador"))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }

    @Test
    @WithMockUser
    void putNaoEncontradoRetorna404() throws Exception {
        UUID id = UUID.randomUUID();
        ManutencaoRequest req = new ManutencaoRequest(
                "X", null, ManutencaoTipo.PREVENTIVA,
                null, null, null, null, null,
                null, null, null, null, ManutencaoStatus.ABERTA, null);
        doThrow(new EntityNotFoundException("Manutencao nao encontrada."))
                .when(manutencaoService).atualizar(eq(id), any());

        mockMvc.perform(put("/api/manutencoes/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Manutencao nao encontrada."));
    }

    @Test
    @WithMockUser
    void deleteDeletaManutencao() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/manutencoes/{id}", id).with(csrf()))
                .andExpect(status().isNoContent());

        verify(manutencaoService).deletar(id);
    }

    @Test
    @WithMockUser
    void deleteNaoEncontradoRetorna404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Manutencao nao encontrada."))
                .when(manutencaoService).deletar(id);

        mockMvc.perform(delete("/api/manutencoes/{id}", id).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Manutencao nao encontrada."));
    }

    @Test
    void getListaSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(get("/api/manutencoes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postSemAutenticacaoRetorna401() throws Exception {
        String body = "{\"titulo\":\"X\",\"tipo\":\"PREVENTIVA\",\"status\":\"ABERTA\"}";
        mockMvc.perform(post("/api/manutencoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    private Manutencao manutencao(UUID id, String titulo) {
        Manutencao m = new Manutencao();
        m.setCondominioId(UUID.randomUUID());
        m.setTitulo(titulo);
        m.setTipo(ManutencaoTipo.PREVENTIVA);
        m.setStatus(ManutencaoStatus.ABERTA);
        m.setCriadoPor(UUID.randomUUID());
        return m;
    }
}
