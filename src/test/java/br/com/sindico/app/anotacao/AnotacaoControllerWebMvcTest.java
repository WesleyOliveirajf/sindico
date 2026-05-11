package br.com.sindico.app.anotacao;

import br.com.sindico.app.config.SecurityConfig;
import br.com.sindico.app.support.WebMvcSecurityTestBase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(controllers = AnotacaoController.class)
@Import(SecurityConfig.class)
class AnotacaoControllerWebMvcTest extends WebMvcSecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnotacaoService anotacaoService;

    @BeforeEach
    void setup() {
        when(anotacaoService.nomeCondominioAtual()).thenReturn("Condominio Piloto");
        when(anotacaoService.listarDoCondominioAtual()).thenReturn(List.of());
    }

    @Test
    @WithMockUser
    void getAnotacoesRetornaView() throws Exception {
        mockMvc.perform(get("/anotacoes"))
                .andExpect(status().isOk())
                .andExpect(view().name("anotacoes"))
                .andExpect(model().attributeExists("anotacoes", "condominioNome", "form"));
    }

    @Test
    @WithMockUser
    void postSemCsrfRetorna403() throws Exception {
        mockMvc.perform(post("/anotacoes")
                        .param("titulo", "Teste")
                        .param("importancia", "NORMAL"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void postSalvaERedireciona() throws Exception {
        when(anotacaoService.criar(any())).thenReturn(new Anotacao());

        mockMvc.perform(post("/anotacoes")
                        .with(csrf())
                        .param("titulo", "Incidente portaria")
                        .param("importancia", "IMPORTANTE")
                        .param("descricao", "Detalhe do ocorrido"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/anotacoes"))
                .andExpect(flash().attribute("mensagem", "Anotacao registrada com sucesso."));

        verify(anotacaoService).criar(any());
    }
}
