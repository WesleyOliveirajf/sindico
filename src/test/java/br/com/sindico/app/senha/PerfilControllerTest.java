package br.com.sindico.app.senha;

import br.com.sindico.app.config.SecurityConfig;
import br.com.sindico.app.security.UsuarioTenantPrincipal;
import br.com.sindico.app.support.WebMvcSecurityTestBase;
import br.com.sindico.app.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = PerfilController.class)
@Import(SecurityConfig.class)
class PerfilControllerTest extends WebMvcSecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PerfilService perfilService;

    private Usuario usuarioFake() {
        Usuario u = new Usuario();
        u.setNome("Joao Silva");
        u.setEmail("joao@email.com");
        return u;
    }

    @Test
    @WithMockUser
    void getPerfilExibeTemplate() throws Exception {
        when(perfilService.usuarioAtual()).thenReturn(usuarioFake());

        mockMvc.perform(get("/perfil"))
                .andExpect(status().isOk())
                .andExpect(view().name("perfil"));
    }

    @Test
    @WithMockUser
    void postDadosComSucessoRedireciona() throws Exception {
        mockMvc.perform(post("/perfil/dados")
                        .with(csrf())
                        .param("nome", "Joao Novo")
                        .param("telefone", "11999990000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/perfil"))
                .andExpect(flash().attributeExists("mensagem"));

        verify(perfilService).atualizarPerfil("Joao Novo", "11999990000");
    }

    @Test
    @WithMockUser
    void postSenhaComSucessoRedireciona() throws Exception {
        mockMvc.perform(post("/perfil/senha")
                        .with(csrf())
                        .param("senhaAtual", "SenhaAntiga1")
                        .param("novaSenha", "SenhaNova1")
                        .param("confirmarSenha", "SenhaNova1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/perfil"))
                .andExpect(flash().attributeExists("mensagem"));

        verify(perfilService).trocarSenha("SenhaAntiga1", "SenhaNova1", "SenhaNova1");
    }

    @Test
    @WithMockUser
    void postSenhaErradaRedirecionaComErro() throws Exception {
        doThrow(new IllegalArgumentException("Senha atual incorreta."))
                .when(perfilService).trocarSenha(any(), any(), any());

        mockMvc.perform(post("/perfil/senha")
                        .with(csrf())
                        .param("senhaAtual", "ErradA1")
                        .param("novaSenha", "NovaSenha1")
                        .param("confirmarSenha", "NovaSenha1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/perfil"))
                .andExpect(flash().attributeExists("erroSenha"));
    }
}
