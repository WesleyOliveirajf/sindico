package br.com.sindico.app.cadastro;

import br.com.sindico.app.config.SecurityConfig;
import br.com.sindico.app.support.WebMvcSecurityTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = CadastroController.class)
@Import(SecurityConfig.class)
class CadastroControllerTest extends WebMvcSecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CadastroService cadastroService;

    @Test
    void getCadastroPublico() throws Exception {
        mockMvc.perform(get("/cadastro"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"));
    }

    @Test
    void postCadastroComSucessoRedireciona() throws Exception {
        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("nome", "Joao Silva")
                        .param("email", "joao@email.com")
                        .param("nomeCondominio", "Residencial das Flores")
                        .param("senha", "Senha123")
                        .param("confirmarSenha", "Senha123")
                        .param("aceitouTermos", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("mensagem"));

        verify(cadastroService).cadastrar(any(CadastroForm.class), anyString(), any(), anyString());
    }

    @Test
    void postCadastroEmailDuplicadoExibeErro() throws Exception {
        doThrow(new IllegalArgumentException("Ja existe uma conta com este e-mail."))
                .when(cadastroService).cadastrar(any(CadastroForm.class), anyString(), any(), anyString());

        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("nome", "Joao Silva")
                        .param("email", "joao@email.com")
                        .param("nomeCondominio", "Residencial das Flores")
                        .param("senha", "Senha123")
                        .param("confirmarSenha", "Senha123")
                        .param("aceitouTermos", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"))
                .andExpect(model().attributeExists("erro"));
    }

    @Test
    void postCadastroSenhasDivergentesRetornaFormulario() throws Exception {
        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("nome", "Joao Silva")
                        .param("email", "joao@email.com")
                        .param("nomeCondominio", "Residencial das Flores")
                        .param("senha", "Senha123")
                        .param("confirmarSenha", "SenhaDiferente")
                        .param("aceitouTermos", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"));
    }

    @Test
    void postCadastroSemCsrfRetorna403() throws Exception {
        mockMvc.perform(post("/cadastro")
                        .param("nome", "Joao Silva")
                        .param("email", "joao@email.com")
                        .param("nomeCondominio", "Residencial das Flores")
                        .param("senha", "Senha123")
                        .param("confirmarSenha", "Senha123")
                        .param("aceitouTermos", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postCadastroValidacaoBeansRetornaFormulario() throws Exception {
        // nome em branco deve falhar na validacao Bean
        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("nome", "")
                        .param("email", "nao-e-email")
                        .param("nomeCondominio", "")
                        .param("senha", "curta")
                        .param("confirmarSenha", "curta"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"))
                .andExpect(model().attributeHasFieldErrors("form", "nome", "email", "nomeCondominio", "senha"));
    }
}
