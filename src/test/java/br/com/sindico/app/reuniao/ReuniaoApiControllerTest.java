package br.com.sindico.app.reuniao;

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

@WebMvcTest(controllers = ReuniaoApiController.class)
@Import(SecurityConfig.class)
class ReuniaoApiControllerTest extends WebMvcSecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReuniaoService reuniaoService;

    @Test
    @WithMockUser
    void getListaReunioes() throws Exception {
        ReuniaoResponse r = reuniaoResponse(UUID.randomUUID(), "Assembleia Geral");
        when(reuniaoService.listarDoCondominioAtual()).thenReturn(List.of(r));

        mockMvc.perform(get("/api/reunioes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Assembleia Geral"))
                .andExpect(jsonPath("$[0].tipo").value("ORDINARIA"))
                .andExpect(jsonPath("$[0].participantes").isArray());
    }

    @Test
    @WithMockUser
    void getListaVaziaRetornaArrayVazio() throws Exception {
        when(reuniaoService.listarDoCondominioAtual()).thenReturn(List.of());

        mockMvc.perform(get("/api/reunioes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser
    void postCriaReuniao() throws Exception {
        ReuniaoRequest req = new ReuniaoRequest(
                "Reuniao de Condominio", ReuniaoTipo.ORDINARIA,
                LocalDateTime.of(2026, 6, 15, 19, 0),
                "Salao de festas", null, "Pauta da reuniao", null, null, null, null);
        ReuniaoResponse r = reuniaoResponse(UUID.randomUUID(), "Reuniao de Condominio");
        when(reuniaoService.criar(any())).thenReturn(r);

        mockMvc.perform(post("/api/reunioes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Reuniao de Condominio"));
    }

    @Test
    @WithMockUser
    void postCriaReuniaoComParticipantes() throws Exception {
        List<ReuniaoRequest.ParticipanteRequest> participantes = List.of(
                new ReuniaoRequest.ParticipanteRequest("Joao Silva", "Sindico", true),
                new ReuniaoRequest.ParticipanteRequest("Maria Oliveira", null, false));
        ReuniaoRequest req = new ReuniaoRequest(
                "Extraordinaria", ReuniaoTipo.EXTRAORDINARIA,
                LocalDateTime.of(2026, 7, 1, 18, 30),
                null, null, null, null, null, null, participantes);
        ReuniaoResponse r = reuniaoResponse(UUID.randomUUID(), "Extraordinaria");
        when(reuniaoService.criar(any())).thenReturn(r);

        mockMvc.perform(post("/api/reunioes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Extraordinaria"));
    }

    @Test
    @WithMockUser
    void postTituloEmRetorna400() throws Exception {
        String body = "{\"titulo\":\"\",\"tipo\":\"ORDINARIA\",\"dataHora\":\"2026-06-15T19:00:00\"}";

        mockMvc.perform(post("/api/reunioes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser
    void postSemTipoRetorna400() throws Exception {
        String body = "{\"titulo\":\"Reuniao X\",\"dataHora\":\"2026-06-15T19:00:00\"}";

        mockMvc.perform(post("/api/reunioes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser
    void postSemDataHoraRetorna400() throws Exception {
        String body = "{\"titulo\":\"Reuniao X\",\"tipo\":\"ORDINARIA\"}";

        mockMvc.perform(post("/api/reunioes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser
    void putAtualizaReuniao() throws Exception {
        UUID id = UUID.randomUUID();
        ReuniaoRequest req = new ReuniaoRequest(
                "Assembleia Atualizada", ReuniaoTipo.EXTRAORDINARIA,
                LocalDateTime.of(2026, 8, 10, 20, 0),
                "Auditorio", null, null, "Resumo aprovado", "Aprovacao de orcamento", null, null);
        ReuniaoResponse r = reuniaoResponse(id, "Assembleia Atualizada");
        when(reuniaoService.atualizar(eq(id), any())).thenReturn(r);

        mockMvc.perform(put("/api/reunioes/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Assembleia Atualizada"));
    }

    @Test
    @WithMockUser
    void putNaoEncontradoRetorna404() throws Exception {
        UUID id = UUID.randomUUID();
        ReuniaoRequest req = new ReuniaoRequest(
                "X", ReuniaoTipo.ORDINARIA,
                LocalDateTime.of(2026, 9, 1, 18, 0),
                null, null, null, null, null, null, null);
        doThrow(new EntityNotFoundException("Reuniao nao encontrada."))
                .when(reuniaoService).atualizar(eq(id), any());

        mockMvc.perform(put("/api/reunioes/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Reuniao nao encontrada."));
    }

    @Test
    @WithMockUser
    void deleteDeletaReuniao() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/reunioes/{id}", id).with(csrf()))
                .andExpect(status().isNoContent());

        verify(reuniaoService).deletar(id);
    }

    @Test
    @WithMockUser
    void deleteNaoEncontradoRetorna404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Reuniao nao encontrada."))
                .when(reuniaoService).deletar(id);

        mockMvc.perform(delete("/api/reunioes/{id}", id).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Reuniao nao encontrada."));
    }

    @Test
    void getListaSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(get("/api/reunioes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postSemAutenticacaoRetorna401() throws Exception {
        String body = "{\"titulo\":\"X\",\"tipo\":\"ORDINARIA\",\"dataHora\":\"2026-06-15T19:00:00\"}";
        mockMvc.perform(post("/api/reunioes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    private ReuniaoResponse reuniaoResponse(UUID id, String titulo) {
        return new ReuniaoResponse(
                id,
                titulo,
                ReuniaoTipo.ORDINARIA,
                LocalDateTime.of(2026, 6, 15, 19, 0),
                "Salao de festas",
                null,
                null,
                null,
                null,
                null,
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}
