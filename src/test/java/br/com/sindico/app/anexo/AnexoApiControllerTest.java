package br.com.sindico.app.anexo;

import br.com.sindico.app.config.SecurityConfig;
import br.com.sindico.app.support.WebMvcSecurityTestBase;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnexoApiController.class)
@Import(SecurityConfig.class)
class AnexoApiControllerTest extends WebMvcSecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnexoService anexoService;

    @Test
    @WithMockUser
    void getListaAnexosDaEntidade() throws Exception {
        UUID entidadeId = UUID.randomUUID();
        AnexoResponse resp = anexoResponse(UUID.randomUUID(), "contrato.pdf", "application/pdf");
        when(anexoService.listar(eq("MANUTENCAO"), eq(entidadeId))).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/anexos")
                        .param("entidadeTipo", "MANUTENCAO")
                        .param("entidadeId", entidadeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomeArquivo").value("contrato.pdf"))
                .andExpect(jsonPath("$[0].mimeType").value("application/pdf"));
    }

    @Test
    @WithMockUser
    void getListaRetornaVazioParaEntidadeSemAnexos() throws Exception {
        UUID entidadeId = UUID.randomUUID();
        when(anexoService.listar(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/anexos")
                        .param("entidadeTipo", "REUNIAO")
                        .param("entidadeId", entidadeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser
    void getListaEntidadeNaoEncontradaRetorna404() throws Exception {
        UUID entidadeId = UUID.randomUUID();
        when(anexoService.listar(any(), any()))
                .thenThrow(new EntityNotFoundException("Registro nao encontrado para o condominio atual."));

        mockMvc.perform(get("/api/anexos")
                        .param("entidadeTipo", "MANUTENCAO")
                        .param("entidadeId", entidadeId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Registro nao encontrado para o condominio atual."));
    }

    @Test
    @WithMockUser
    void postUploadAnexoComSucesso() throws Exception {
        UUID entidadeId = UUID.randomUUID();
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "foto.jpg", "image/jpeg", "conteudo da imagem".getBytes());
        AnexoResponse resp = anexoResponse(UUID.randomUUID(), "foto.jpg", "image/jpeg");
        when(anexoService.upload(eq("MANUTENCAO"), eq(entidadeId), any())).thenReturn(resp);

        mockMvc.perform(multipart("/api/anexos")
                        .file(arquivo)
                        .param("entidadeTipo", "MANUTENCAO")
                        .param("entidadeId", entidadeId.toString())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeArquivo").value("foto.jpg"))
                .andExpect(jsonPath("$.mimeType").value("image/jpeg"));
    }

    @Test
    @WithMockUser
    void postUploadEntidadeInvalidaRetorna400() throws Exception {
        UUID entidadeId = UUID.randomUUID();
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "doc.pdf", "application/pdf", "conteudo".getBytes());
        when(anexoService.upload(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Entidade invalida"));

        mockMvc.perform(multipart("/api/anexos")
                        .file(arquivo)
                        .param("entidadeTipo", "INVALIDO")
                        .param("entidadeId", entidadeId.toString())
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Entidade invalida"));
    }

    @Test
    @WithMockUser
    void postUploadEntidadeNaoEncontradaRetorna404() throws Exception {
        UUID entidadeId = UUID.randomUUID();
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "relatorio.pdf", "application/pdf", "bytes".getBytes());
        when(anexoService.upload(any(), any(), any()))
                .thenThrow(new EntityNotFoundException("Registro nao encontrado para o condominio atual."));

        mockMvc.perform(multipart("/api/anexos")
                        .file(arquivo)
                        .param("entidadeTipo", "MANUTENCAO")
                        .param("entidadeId", entidadeId.toString())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Registro nao encontrado para o condominio atual."));
    }

    @Test
    @WithMockUser
    void getDownloadAnexoComSucesso() throws Exception {
        UUID anexoId = UUID.randomUUID();
        byte[] conteudo = "conteudo do pdf".getBytes();
        AnexoService.DownloadPayload payload = new AnexoService.DownloadPayload(
                new ByteArrayResource(conteudo), "documento.pdf", "application/pdf");
        when(anexoService.download(eq(anexoId))).thenReturn(payload);

        mockMvc.perform(get("/api/anexos/{anexoId}/download", anexoId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(conteudo));
    }

    @Test
    @WithMockUser
    void getDownloadAnexoNaoEncontradoRetorna404() throws Exception {
        UUID anexoId = UUID.randomUUID();
        when(anexoService.download(eq(anexoId)))
                .thenThrow(new EntityNotFoundException("Anexo nao encontrado."));

        mockMvc.perform(get("/api/anexos/{anexoId}/download", anexoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Anexo nao encontrado."));
    }

    @Test
    void getListaSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(get("/api/anexos")
                        .param("entidadeTipo", "MANUTENCAO")
                        .param("entidadeId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postUploadSemAutenticacaoRetorna401() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "foto.jpg", "image/jpeg", "bytes".getBytes());
        mockMvc.perform(multipart("/api/anexos")
                        .file(arquivo)
                        .param("entidadeTipo", "MANUTENCAO")
                        .param("entidadeId", UUID.randomUUID().toString())
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    private AnexoResponse anexoResponse(UUID id, String nomeArquivo, String mimeType) {
        return new AnexoResponse(id, nomeArquivo, mimeType, 1024L, LocalDateTime.now());
    }
}
