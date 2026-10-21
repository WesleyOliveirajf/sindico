package br.com.sindico.app.financeiro;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financeiro")
public class FinanceiroApiController {

    private final FinanceiroService financeiroService;

    public FinanceiroApiController(FinanceiroService financeiroService) {
        this.financeiroService = financeiroService;
    }

    @GetMapping("/saldo")
    public SaldoAjusteResponse resumoSaldo(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano) {
        return financeiroService.resumoSaldo(mes, ano);
    }

    @PutMapping("/saldo-ajuste")
    public SaldoAjusteResponse definirSaldoReal(@Valid @RequestBody SaldoAjusteRequest request) {
        return financeiroService.definirSaldoReal(request);
    }
}
