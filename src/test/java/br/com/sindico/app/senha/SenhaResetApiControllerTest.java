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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SenhaResetApiController.class)
@Import(SecurityConfig.class)
class SenhaResetApiControllerTest extends WebMvcSecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SenhaResetService senhaResetService;

    @Test
    void esqueciRetorna202SemAutenticacao() throws Exception {
        mockMvc.perform(post("/api/senha/esqueci")
                        .contentType("application/json")
                        .content("{\"email\":\"usuario@email.com\"}"))
                .andExpect(status().isAccepted());

        verify(senhaResetService).solicitarReset(eq("usuario@email.com"), any());
    }

    @Test
    void esqueciComEmailInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/api/senha/esqueci")
                        .contentType("application/json")
                        .content("{\"email\":\"nao-e-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validarTokenValidoRetorna204() throws Exception {
        mockMvc.perform(get("/api/senha/validar").param("token", "token-valido"))
                .andExpect(status().isNoContent());
    }

    @Test
    void validarTokenInvalidoRetorna400ComMensagem() throws Exception {
        doThrow(new IllegalArgumentException("Link invalido ou expirado. Solicite um novo."))
                .when(senhaResetService).validarToken(any());

        mockMvc.perform(get("/api/senha/validar").param("token", "token-ruim"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Link invalido ou expirado. Solicite um novo."));
    }

    @Test
    void redefinirRetorna204() throws Exception {
        mockMvc.perform(post("/api/senha/redefinir")
                        .contentType("application/json")
                        .content("{\"token\":\"t\",\"novaSenha\":\"nova1234\",\"confirmarSenha\":\"nova1234\"}"))
                .andExpect(status().isNoContent());

        verify(senhaResetService).redefinirSenha("t", "nova1234", "nova1234");
    }
}
