package br.com.sindico.app.condominio;

import br.com.sindico.app.config.SecurityConfig;
import br.com.sindico.app.support.WebMvcSecurityTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CondominioApiController.class)
@Import(SecurityConfig.class)
class CondominioApiControllerTest extends WebMvcSecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CondominioService condominioService;

    @Test
    void getSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(get("/api/condominio"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getRetornaDadosDoCondominioAtual() throws Exception {
        CondominioForm form = new CondominioForm();
        form.setNome("Residencial Alfa");
        form.setEndereco("Rua A, 10");
        when(condominioService.buscarFormAtual()).thenReturn(form);

        mockMvc.perform(get("/api/condominio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Residencial Alfa"))
                .andExpect(jsonPath("$.endereco").value("Rua A, 10"));
    }

    @Test
    @WithMockUser
    void putSemNomeRetorna400() throws Exception {
        mockMvc.perform(put("/api/condominio")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void putAtualizaCondominio() throws Exception {
        Condominio atualizado = new Condominio();
        atualizado.setNome("Residencial Beta");
        when(condominioService.atualizar(any(CondominioForm.class))).thenReturn(atualizado);

        mockMvc.perform(put("/api/condominio")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"nome\":\"Residencial Beta\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Residencial Beta"));
    }
}
