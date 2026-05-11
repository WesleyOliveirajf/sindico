package br.com.sindico.app.anotacao;

import br.com.sindico.app.config.SecurityConfig;
import br.com.sindico.app.support.WebMvcSecurityTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
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

@WebMvcTest(controllers = AnotacaoApiController.class)
@Import(SecurityConfig.class)
class AnotacaoApiControllerTest extends WebMvcSecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnotacaoService anotacaoService;

    @Test
    @WithMockUser
    void getListaAnotacoes() throws Exception {
        Anotacao a = anotacao(UUID.randomUUID(), "Reuniao anual");
        when(anotacaoService.listarComFiltros(any(), any(), any())).thenReturn(List.of(a));

        mockMvc.perform(get("/api/anotacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Reuniao anual"));
    }

    @Test
    @WithMockUser
    void postCriaAnotacao() throws Exception {
        AnotacaoRequest req = new AnotacaoRequest("Novo teto", null, null, null, AnotacaoImportancia.IMPORTANTE);
        Anotacao a = anotacao(UUID.randomUUID(), "Novo teto");
        when(anotacaoService.criar(any())).thenReturn(a);

        mockMvc.perform(post("/api/anotacoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Novo teto"));
    }

    @Test
    @WithMockUser
    void postInvalidoRetorna400() throws Exception {
        String body = "{\"titulo\":\"\",\"importancia\":\"NORMAL\"}";

        mockMvc.perform(post("/api/anotacoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser
    void putAtualizaAnotacao() throws Exception {
        UUID id = UUID.randomUUID();
        AnotacaoRequest req = new AnotacaoRequest("Titulo atualizado", null, null, null, AnotacaoImportancia.CRITICO);
        Anotacao a = anotacao(id, "Titulo atualizado");
        when(anotacaoService.atualizar(eq(id), any())).thenReturn(a);

        mockMvc.perform(put("/api/anotacoes/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Titulo atualizado"));
    }

    @Test
    @WithMockUser
    void putNaoEncontradoRetorna404() throws Exception {
        UUID id = UUID.randomUUID();
        AnotacaoRequest req = new AnotacaoRequest("X", null, null, null, AnotacaoImportancia.NORMAL);
        doThrow(new EntityNotFoundException("Anotacao nao encontrada."))
                .when(anotacaoService).atualizar(eq(id), any());

        mockMvc.perform(put("/api/anotacoes/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Anotacao nao encontrada."));
    }

    @Test
    @WithMockUser
    void deleteDeletaAnotacao() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/anotacoes/{id}", id).with(csrf()))
                .andExpect(status().isNoContent());

        verify(anotacaoService).deletar(id);
    }

    @Test
    @WithMockUser
    void deleteNaoEncontradoRetorna404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Anotacao nao encontrada."))
                .when(anotacaoService).deletar(id);

        mockMvc.perform(delete("/api/anotacoes/{id}", id).with(csrf()))
                .andExpect(status().isNotFound());
    }

    private Anotacao anotacao(UUID id, String titulo) {
        Anotacao a = new Anotacao();
        // use reflection-free approach: set via setters, id stays null from JPA perspective in tests
        a.setCondominioId(UUID.randomUUID());
        a.setTitulo(titulo);
        a.setImportancia(AnotacaoImportancia.NORMAL);
        return a;
    }
}
