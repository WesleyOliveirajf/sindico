package br.com.sindico.app.dashboard;

import br.com.sindico.app.anotacao.AnotacaoService;
import br.com.sindico.app.compromisso.Compromisso;
import br.com.sindico.app.compromisso.CompromissoService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerWebMvcTest extends WebMvcSecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompromissoService compromissoService;

    @MockBean
    private AnotacaoService anotacaoService;

    @BeforeEach
    void setup() {
        when(anotacaoService.nomeCondominioAtual()).thenReturn("Condominio Piloto");
        when(compromissoService.totalManutencoesAgendadas()).thenReturn(0L);
        when(compromissoService.totalReunioesAgendadas()).thenReturn(0L);
        when(compromissoService.totalPendencias()).thenReturn(0L);
        when(compromissoService.proximos()).thenReturn(List.of());
    }

    @Test
    @WithMockUser
    void deveExigirCsrfNoPostCompromissos() throws Exception {
        mockMvc.perform(post("/compromissos")
                        .param("titulo", "Revisao")
                        .param("tipo", "MANUTENCAO")
                        .param("inicioEm", "2026-05-10T09:00")
                        .param("fimEm", "2026-05-10T10:00"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void deveRetornarDashboardQuandoDataFinalMenorOuIgualDataInicial() throws Exception {
        doThrow(new IllegalArgumentException("Data final deve ser maior que a data inicial"))
                .when(compromissoService)
                .criar(any());

        mockMvc.perform(post("/compromissos")
                        .with(csrf())
                        .param("titulo", "Revisao")
                        .param("tipo", "MANUTENCAO")
                        .param("inicioEm", "2026-05-10T10:00")
                        .param("fimEm", "2026-05-10T09:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    @WithMockUser
    void deveSalvarComSucessoERedirecionarComMensagem() throws Exception {
        when(compromissoService.criar(any())).thenReturn(new Compromisso());

        mockMvc.perform(post("/compromissos")
                        .with(csrf())
                        .param("titulo", "Revisao")
                        .param("tipo", "MANUTENCAO")
                        .param("inicioEm", "2026-05-10T09:00")
                        .param("fimEm", "2026-05-10T10:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("mensagem", "Lembrete salvo com sucesso."));

        verify(compromissoService).criar(any());
    }
}
