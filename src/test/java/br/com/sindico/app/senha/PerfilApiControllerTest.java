package br.com.sindico.app.senha;

import br.com.sindico.app.config.SecurityConfig;
import br.com.sindico.app.support.WebMvcSecurityTestBase;
import br.com.sindico.app.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(controllers = PerfilApiController.class)
@Import(SecurityConfig.class)
class PerfilApiControllerTest extends WebMvcSecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PerfilService perfilService;

    @Test
    @WithMockUser
    void getRetornaPerfilAtual() throws Exception {
        Usuario u = new Usuario();
        u.setNome("Maria");
        u.setEmail("maria@email.com");
        when(perfilService.usuarioAtual()).thenReturn(u);

        mockMvc.perform(get("/api/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.email").value("maria@email.com"));
    }

    @Test
    @WithMockUser
    void putDadosSemNomeRetorna400() throws Exception {
        mockMvc.perform(put("/api/perfil/dados")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void putDadosDelegaParaServico() throws Exception {
        Usuario u = new Usuario();
        u.setNome("Maria Silva");
        when(perfilService.usuarioAtual()).thenReturn(u);

        mockMvc.perform(put("/api/perfil/dados")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"nome\":\"Maria Silva\",\"telefone\":\"11999990000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva"));

        verify(perfilService).atualizarPerfil("Maria Silva", "11999990000");
    }

    @Test
    @WithMockUser
    void postSenhaRetorna204() throws Exception {
        mockMvc.perform(post("/api/perfil/senha")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"senhaAtual\":\"atual123\",\"novaSenha\":\"nova1234\",\"confirmarSenha\":\"nova1234\"}"))
                .andExpect(status().isNoContent());

        verify(perfilService).trocarSenha("atual123", "nova1234", "nova1234");
    }

    @Test
    @WithMockUser
    void postSenhaIncorretaRetorna400ComMensagem() throws Exception {
        doThrow(new IllegalArgumentException("Senha atual incorreta."))
                .when(perfilService).trocarSenha(eq("errada"), eq("nova1234"), eq("nova1234"));

        mockMvc.perform(post("/api/perfil/senha")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"senhaAtual\":\"errada\",\"novaSenha\":\"nova1234\",\"confirmarSenha\":\"nova1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Senha atual incorreta."));
    }
}
