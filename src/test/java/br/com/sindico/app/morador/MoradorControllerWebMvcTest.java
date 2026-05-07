package br.com.sindico.app.morador;

import br.com.sindico.app.config.SecurityConfig;
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

@WebMvcTest(controllers = MoradorController.class)
@Import(SecurityConfig.class)
class MoradorControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MoradorGestaoService moradorGestaoService;

    @BeforeEach
    void setup() {
        when(moradorGestaoService.nomeCondominioAtual()).thenReturn("Condominio Piloto");
        when(moradorGestaoService.listarUnidades()).thenReturn(List.of());
        when(moradorGestaoService.listarMoradoresAtivos()).thenReturn(List.of());
    }

    @Test
    @WithMockUser
    void getMoradores() throws Exception {
        mockMvc.perform(get("/moradores"))
                .andExpect(status().isOk())
                .andExpect(view().name("moradores"))
                .andExpect(model().attributeExists("condominioNome", "unidades", "moradores", "formUnidade", "formMorador"));
    }

    @Test
    @WithMockUser
    void postUnidadeSemCsrf() throws Exception {
        mockMvc.perform(post("/moradores/unidades").param("numero", "101"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void postMoradorComSucesso() throws Exception {
        mockMvc.perform(post("/moradores")
                        .with(csrf())
                        .param("unidadeId", UUID.randomUUID().toString())
                        .param("nome", "Maria")
                        .param("papel", "PROPRIETARIO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/moradores"))
                .andExpect(flash().attribute("mensagem", "Morador cadastrado com sucesso."));

        verify(moradorGestaoService).criarMorador(any());
    }
}
