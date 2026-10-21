package br.com.sindico.app.financeiro;

import br.com.sindico.app.config.SecurityConfig;
import br.com.sindico.app.support.WebMvcSecurityTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FinanceiroApiController.class)
@Import(SecurityConfig.class)
class FinanceiroApiControllerTest extends WebMvcSecurityTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FinanceiroService financeiroService;

    @Test
    @WithMockUser
    void should_return_saldo_resumo() throws Exception {
        when(financeiroService.resumoSaldo(10, 2026)).thenReturn(new SaldoAjusteResponse(
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(1200),
                BigDecimal.valueOf(700),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(800),
                "Conciliação bancária"));

        mockMvc.perform(get("/api/financeiro/saldo").param("mes", "10").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoCalculado").value(700))
                .andExpect(jsonPath("$.ajusteSaldo").value(100))
                .andExpect(jsonPath("$.saldoReal").value(800));

        verify(financeiroService).resumoSaldo(10, 2026);
    }

    @Test
    @WithMockUser
    void should_define_saldo_real() throws Exception {
        SaldoAjusteRequest req = new SaldoAjusteRequest(10, 2026, BigDecimal.valueOf(9500), "Saldo em conta");
        when(financeiroService.definirSaldoReal(any())).thenReturn(new SaldoAjusteResponse(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(9500),
                BigDecimal.valueOf(9500),
                "Saldo em conta"));

        mockMvc.perform(put("/api/financeiro/saldo-ajuste")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoReal").value(9500));

        verify(financeiroService).definirSaldoReal(any(SaldoAjusteRequest.class));
    }
}
