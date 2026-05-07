package br.com.sindico.app.prestador;

import br.com.sindico.app.config.SecurityConfig;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = PrestadorServicoController.class)
@Import(SecurityConfig.class)
class PrestadorServicoControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PrestadorServicoService prestadorServicoService;

    @BeforeEach
    void setup() {
        when(prestadorServicoService.nomeCondominioAtual()).thenReturn("Condominio Piloto");
        when(prestadorServicoService.listarDoCondominioAtual()).thenReturn(List.of());
    }

    @Test
    @WithMockUser
    void getPrestadoresRetornaView() throws Exception {
        mockMvc.perform(get("/prestadores"))
                .andExpect(status().isOk())
                .andExpect(view().name("prestadores"))
                .andExpect(model().attributeExists("prestadores", "condominioNome", "form"));
    }

    @Test
    @WithMockUser
    void postSemCsrfRetorna403() throws Exception {
        mockMvc.perform(post("/prestadores")
                        .param("nome", "Eletrica Silva")
                        .param("telefone", "11988887777"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void postComSucessoRedireciona() throws Exception {
        when(prestadorServicoService.criar(any())).thenReturn(new PrestadorServico());

        mockMvc.perform(post("/prestadores")
                        .with(csrf())
                        .param("nome", "Eletrica Silva")
                        .param("telefone", "11988887777")
                        .param("historicoServicos", "Troca de lampadas bloco A"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prestadores"))
                .andExpect(flash().attribute("mensagem", "Prestador cadastrado com sucesso."));

        verify(prestadorServicoService).criar(any());
    }

    @Test
    @WithMockUser
    void postAtualizacaoComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        when(prestadorServicoService.atualizar(eq(id), any())).thenReturn(new PrestadorServico());

        mockMvc.perform(post("/prestadores/{id}", id)
                        .with(csrf())
                        .param("nome", "Hidraulica Central")
                        .param("telefone", "11999990000")
                        .param("historicoServicos", "Reparo de vazamento no bloco B"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prestadores"))
                .andExpect(flash().attribute("mensagem", "Prestador atualizado com sucesso."));

        verify(prestadorServicoService).atualizar(eq(id), any());
    }

    @Test
    @WithMockUser
    void postAtualizacaoQuandoNaoEncontrado() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Prestador nao encontrado."))
                .when(prestadorServicoService)
                .atualizar(eq(id), any());

        mockMvc.perform(post("/prestadores/{id}", id)
                        .with(csrf())
                        .param("nome", "Hidraulica Central")
                        .param("telefone", "11999990000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prestadores"))
                .andExpect(flash().attribute("mensagemErro", "Prestador nao encontrado."));
    }

    @Test
    @WithMockUser
    void postInativacaoComSucesso() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/prestadores/{id}/inativar", id)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prestadores"))
                .andExpect(flash().attribute("mensagem", "Prestador inativado com sucesso."));

        verify(prestadorServicoService).inativar(id);
    }
}
