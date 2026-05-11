package br.com.sindico.app.senha;

import br.com.sindico.app.config.SecurityConfig;
import br.com.sindico.app.support.WebMvcSecurityTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(controllers = SenhaResetController.class)
@Import(SecurityConfig.class)
class SenhaResetControllerTest extends WebMvcSecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SenhaResetService senhaResetService;

    @Test
    void getEsqueciSenhaPublico() throws Exception {
        mockMvc.perform(get("/esqueci-senha"))
                .andExpect(status().isOk())
                .andExpect(view().name("esqueci-senha"));
    }

    @Test
    void postEsqueciSenhaRedirecionaComMensagemGenerica() throws Exception {
        mockMvc.perform(post("/esqueci-senha")
                        .with(csrf())
                        .param("email", "usuario@email.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/esqueci-senha"))
                .andExpect(flash().attributeExists("mensagem"));

        verify(senhaResetService).solicitarReset(eq("usuario@email.com"), any());
    }

    @Test
    void getRedefinirSenhaTokenValidoExibeFormulario() throws Exception {
        String token = "token-valido-abc";
        // validarToken nao lanca excecao = token valido
        mockMvc.perform(get("/redefinir-senha").param("token", token))
                .andExpect(status().isOk())
                .andExpect(view().name("redefinir-senha"))
                .andExpect(model().attribute("token", token));
    }

    @Test
    void getRedefinirSenhaTokenInvalidoExibeErro() throws Exception {
        doThrow(new IllegalArgumentException("Link invalido ou expirado."))
                .when(senhaResetService).validarToken(any());

        mockMvc.perform(get("/redefinir-senha").param("token", "token-ruim"))
                .andExpect(status().isOk())
                .andExpect(view().name("redefinir-senha"))
                .andExpect(model().attributeExists("erro"));
    }

    @Test
    void postRedefinirSenhaComSucessoRedireciona() throws Exception {
        mockMvc.perform(post("/redefinir-senha")
                        .with(csrf())
                        .param("token", "tok123")
                        .param("novaSenha", "NovaSenha1")
                        .param("confirmarSenha", "NovaSenha1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("mensagem"));

        verify(senhaResetService).redefinirSenha("tok123", "NovaSenha1", "NovaSenha1");
    }

    @Test
    void postRedefinirSenhaTokenInvalidoExibeErro() throws Exception {
        doThrow(new IllegalArgumentException("Link invalido ou expirado. Solicite um novo."))
                .when(senhaResetService).redefinirSenha(any(), any(), any());

        mockMvc.perform(post("/redefinir-senha")
                        .with(csrf())
                        .param("token", "tok-ruim")
                        .param("novaSenha", "NovaSenha1")
                        .param("confirmarSenha", "NovaSenha1"))
                .andExpect(status().isOk())
                .andExpect(view().name("redefinir-senha"))
                .andExpect(model().attributeExists("erro"));
    }
}
