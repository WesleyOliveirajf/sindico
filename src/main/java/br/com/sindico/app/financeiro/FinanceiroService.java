package br.com.sindico.app.financeiro;

import br.com.sindico.app.gasto.Gasto;
import br.com.sindico.app.gasto.GastoParcela;
import br.com.sindico.app.gasto.GastoService;
import br.com.sindico.app.recebimento.Recebimento;
import br.com.sindico.app.recebimento.RecebimentoService;
import br.com.sindico.app.security.SecurityUtils;
import br.com.sindico.app.security.TenantAccessor;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceiroService {

    private final GastoService gastoService;
    private final RecebimentoService recebimentoService;
    private final SaldoAjustePeriodoRepository saldoAjusteRepository;
    private final TenantAccessor tenantAccessor;

    public FinanceiroService(
            GastoService gastoService,
            RecebimentoService recebimentoService,
            SaldoAjustePeriodoRepository saldoAjusteRepository,
            TenantAccessor tenantAccessor) {
        this.gastoService = gastoService;
        this.recebimentoService = recebimentoService;
        this.saldoAjusteRepository = saldoAjusteRepository;
        this.tenantAccessor = tenantAccessor;
    }

    @Transactional(readOnly = true)
    public SaldoAjusteResponse resumoSaldo(Integer mes, Integer ano) {
        BigDecimal totalGastos = totalGastos(mes, ano);
        BigDecimal totalRecebimentos = totalRecebimentos(mes, ano);
        BigDecimal saldoCalculado = totalRecebimentos.subtract(totalGastos);
        BigDecimal ajusteSaldo = ajusteParaFiltro(mes, ano);
        String observacao = observacaoParaFiltro(mes, ano);
        return new SaldoAjusteResponse(
                totalGastos,
                totalRecebimentos,
                saldoCalculado,
                ajusteSaldo,
                saldoCalculado.add(ajusteSaldo),
                observacao);
    }

    @Transactional
    public SaldoAjusteResponse definirSaldoReal(SaldoAjusteRequest req) {
        BigDecimal totalGastos = totalGastos(req.mes(), req.ano());
        BigDecimal totalRecebimentos = totalRecebimentos(req.mes(), req.ano());
        BigDecimal saldoCalculado = totalRecebimentos.subtract(totalGastos);
        BigDecimal novoAjuste = req.saldoRealDesejado().subtract(saldoCalculado);

        UUID condominioId = tenantAccessor.condominioAtual();
        SaldoAjustePeriodo registro = saldoAjusteRepository
                .findByCondominioIdAndAnoAndMes(condominioId, req.ano(), req.mes())
                .orElseGet(SaldoAjustePeriodo::new);
        if (registro.getId() == null) {
            registro.setCondominioId(condominioId);
            registro.setAno(req.ano());
            registro.setMes(req.mes());
        }
        registro.setValor(novoAjuste);
        registro.setObservacao(SecurityUtils.blankToNull(req.observacao()));
        registro.setAtualizadoPor(SecurityUtils.usuarioAtualId());
        saldoAjusteRepository.save(registro);

        return new SaldoAjusteResponse(
                totalGastos,
                totalRecebimentos,
                saldoCalculado,
                novoAjuste,
                req.saldoRealDesejado(),
                registro.getObservacao());
    }

    private BigDecimal ajusteParaFiltro(Integer mes, Integer ano) {
        if (mes != null && ano != null) {
            return saldoAjusteRepository
                    .findByCondominioIdAndAnoAndMes(tenantAccessor.condominioAtual(), ano, mes)
                    .map(SaldoAjustePeriodo::getValor)
                    .orElse(BigDecimal.ZERO);
        }
        return saldoAjusteRepository.somarAjustes(tenantAccessor.condominioAtual(), ano, mes);
    }

    private String observacaoParaFiltro(Integer mes, Integer ano) {
        if (mes == null || ano == null) {
            return null;
        }
        return saldoAjusteRepository
                .findByCondominioIdAndAnoAndMes(tenantAccessor.condominioAtual(), ano, mes)
                .map(SaldoAjustePeriodo::getObservacao)
                .orElse(null);
    }

    private BigDecimal totalGastos(Integer mes, Integer ano) {
        List<Gasto> gastos = gastoService.listar(mes, ano, null);
        return gastos.stream().map(this::valorContabilizado).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal valorContabilizado(Gasto gasto) {
        if (!gasto.isParcelado()) {
            return gasto.getValor();
        }
        return gastoService.parcelas(gasto.getId()).stream()
                .filter(GastoParcela::isPaga)
                .map(GastoParcela::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalRecebimentos(Integer mes, Integer ano) {
        return recebimentoService.listar(mes, ano, null).stream()
                .map(Recebimento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
