package br.com.sindico.app.morador;

import br.com.sindico.app.config.SecurityConfig;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MoradorApiController.class)
@Import(SecurityConfig.class)
class MoradorApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MoradorGestaoService moradorGestaoService;

    @Test
    @WithMockUser
    void getUnidadesRetornaLista() throws Exception {
        Unidade u = unidade(UUID.randomUUID(), "A", "101");
        when(moradorGestaoService.listarUnidades()).thenReturn(List.of(u));

        mockMvc.perform(get("/api/unidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("101"));
    }

    @Test
    @WithMockUser
    void getMoradoresRetornaLista() throws Exception {
        when(moradorGestaoService.listarMoradoresAtivos()).thenReturn(List.of());

        mockMvc.perform(get("/api/moradores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser
    void postMoradorInvalidoRetorna400() throws Exception {
        String body = "{\"unidadeId\":null,\"nome\":\"\",\"papel\":\"PROPRIETARIO\"}";

        mockMvc.perform(post("/api/moradores")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void putMoradorAtualiza() throws Exception {
        UUID id = UUID.randomUUID();
        UUID unidadeId = UUID.randomUUID();
        MoradorRequest req = new MoradorRequest(unidadeId, "Carlos Silva", null, "11999990000", MoradorPapel.INQUILINO, null);
        Morador m = morador(id, "Carlos Silva", unidade(unidadeId, "", "202"));
        when(moradorGestaoService.atualizarMorador(eq(id), any())).thenReturn(m);

        mockMvc.perform(put("/api/moradores/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos Silva"));
    }

    @Test
    @WithMockUser
    void putMoradorNaoEncontradoRetorna404() throws Exception {
        UUID id = UUID.randomUUID();
        MoradorRequest req = new MoradorRequest(UUID.randomUUID(), "X", null, null, MoradorPapel.OUTRO, null);
        doThrow(new EntityNotFoundException("Morador nao encontrado."))
                .when(moradorGestaoService).atualizarMorador(eq(id), any());

        mockMvc.perform(put("/api/moradores/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Morador nao encontrado."));
    }

    @Test
    @WithMockUser
    void postInativarMorador() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/moradores/{id}/inativar", id).with(csrf()))
                .andExpect(status().isNoContent());

        verify(moradorGestaoService).inativarMorador(id);
    }

    @Test
    @WithMockUser
    void postInativarNaoEncontradoRetorna404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Morador nao encontrado."))
                .when(moradorGestaoService).inativarMorador(id);

        mockMvc.perform(post("/api/moradores/{id}/inativar", id).with(csrf()))
                .andExpect(status().isNotFound());
    }

    private Unidade unidade(UUID id, String bloco, String numero) {
        Unidade u = new Unidade();
        u.setCondominioId(UUID.randomUUID());
        u.setBloco(bloco);
        u.setNumero(numero);
        return u;
    }

    private Morador morador(UUID id, String nome, Unidade unidade) {
        Morador m = new Morador();
        m.setNome(nome);
        m.setUnidade(unidade);
        m.setPapel(MoradorPapel.PROPRIETARIO);
        m.setAtivo(true);
        return m;
    }
}
