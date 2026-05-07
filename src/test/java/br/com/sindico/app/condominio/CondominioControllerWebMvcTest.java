package br.com.sindico.app.condominio;

import br.com.sindico.app.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

@WebMvcTest(controllers = CondominioController.class)
@Import(SecurityConfig.class)
class CondominioControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CondominioService condominioService;

    @BeforeEach
    void setup() {
        CondominioForm form = new CondominioForm();
        form.setNome("Condominio Piloto");
        form.setEndereco("Rua Central, 100");
        when(condominioService.buscarFormAtual()).thenReturn(form);
        when(condominioService.atualizar(any())).thenReturn(new Condominio());
    }

    @Test
    @WithMockUser
    void getCondominioRetornaViewComFormulario() throws Exception {
        mockMvc.perform(get("/condominio"))
                .andExpect(status().isOk())
                .andExpect(view().name("condominio"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    @WithMockUser
    void postSemCsrfRetorna403() throws Exception {
        mockMvc.perform(post("/condominio")
                        .param("nome", "Residencial Jardim"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void postInvalidoRetornaFormulario() throws Exception {
        mockMvc.perform(post("/condominio")
                        .with(csrf())
                        .param("nome", "")
                        .param("endereco", "Rua Central, 100"))
                .andExpect(status().isOk())
                .andExpect(view().name("condominio"));

        verify(condominioService, never()).atualizar(any());
    }

    @Test
    @WithMockUser
    void postValidoAtualizaERedirecionaComMensagem() throws Exception {
        mockMvc.perform(post("/condominio")
                        .with(csrf())
                        .param("nome", "Residencial Jardim")
                        .param("endereco", "Rua Central, 100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/condominio"))
                .andExpect(flash().attribute("mensagem", "Condominio atualizado com sucesso."));

        verify(condominioService).atualizar(any());
    }
}
