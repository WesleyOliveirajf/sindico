package br.com.sindico.app.gasto;

import br.com.sindico.app.security.SecurityUtils;
import br.com.sindico.app.security.TenantAccessor;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GastoService {

    private final GastoRepository gastoRepository;
    private final TenantAccessor tenantAccessor;
    private final GastoParcelaRepository parcelaRepository;

    public GastoService(GastoRepository gastoRepository, TenantAccessor tenantAccessor, GastoParcelaRepository parcelaRepository) {
        this.gastoRepository = gastoRepository;
        this.tenantAccessor = tenantAccessor;
        this.parcelaRepository = parcelaRepository;
    }

    @Transactional(readOnly = true)
    public List<Gasto> listar(Integer mes, Integer ano, GastoTipo tipo) {
        // Filtros aplicados via query JPQL — não carrega tudo em memória (BUG-001 corrigido)
        return gastoRepository.filtrar(tenantAccessor.condominioAtual(), mes, ano, tipo);
    }

    @Transactional
    public Gasto criar(GastoRequest req) {
        Gasto g = new Gasto();
        apply(g, req);
        g.setCondominioId(tenantAccessor.condominioAtual());
        g.setCriadoPor(SecurityUtils.usuarioAtualId());
        Gasto salvo = gastoRepository.save(g);
        gerarParcelas(salvo, req);
        return salvo;
    }

    @Transactional
    public Gasto atualizar(UUID id, GastoRequest req) {
        Gasto g = gastoRepository.findById(id)
                .filter(x -> x.getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new EntityNotFoundException("Gasto nao encontrado."));
        apply(g, req);
        parcelaRepository.deleteByGastoId(g.getId());
        gerarParcelas(g, req);
        return gastoRepository.save(g);
    }

    @Transactional(readOnly = true)
    public List<GastoParcela> parcelas(UUID gastoId) {
        validarGasto(gastoId);
        return parcelaRepository.findByGastoIdOrderByNumero(gastoId);
    }

    @Transactional
    public GastoParcela marcarParcela(UUID gastoId, Integer numero, boolean paga) {
        validarGasto(gastoId);
        GastoParcela parcela = parcelaRepository.findByGastoIdAndNumero(gastoId, numero)
                .orElseThrow(() -> new EntityNotFoundException("Parcela nao encontrada."));
        parcela.setPaga(paga);
        parcela.setPagaEm(paga ? LocalDateTime.now() : null);
        return parcelaRepository.save(parcela);
    }

    private Gasto validarGasto(UUID id) {
        return gastoRepository.findById(id)
                .filter(x -> x.getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new EntityNotFoundException("Gasto nao encontrado."));
    }

    private void gerarParcelas(Gasto gasto, GastoRequest req) {
        if (!req.parcelado()) return;
        BigDecimal valorParcela = req.valorParcela();
        if (valorParcela == null) throw new IllegalArgumentException("Informe o valor de cada parcela.");
        gasto.setValorParcela(valorParcela);
        for (int numero = 1; numero <= req.parcelaTotal(); numero++) {
            GastoParcela parcela = new GastoParcela();
            parcela.setGastoId(gasto.getId());
            parcela.setNumero(numero);
            parcela.setValor(numero == req.parcelaTotal()
                    ? gasto.getValor().subtract(valorParcela.multiply(BigDecimal.valueOf(req.parcelaTotal() - 1L)))
                    : valorParcela);
            parcelaRepository.save(parcela);
        }
    }

    @Transactional
    public void deletar(UUID id) {
        Gasto g = gastoRepository.findById(id)
                .filter(x -> x.getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new EntityNotFoundException("Gasto nao encontrado."));
        gastoRepository.delete(g);
    }

    private static void apply(Gasto g, GastoRequest req) {
        g.setDescricao(req.descricao().trim());
        g.setTipo(req.tipo());
        g.setValor(req.valor());
        g.setValorParcela(req.parcelado() ? req.valorParcela() : null);
        g.setDataGasto(req.dataGasto());
        g.setFixo(req.fixo());
        g.setParcelado(req.parcelado());
        if (req.parcelado()) {
            if (req.parcelaAtual() == null || req.parcelaTotal() == null) {
                throw new IllegalArgumentException("Informe a parcela atual e o total de parcelas.");
            }
            if (req.parcelaAtual() < 1 || req.parcelaTotal() < 1) {
                throw new IllegalArgumentException("Parcela atual e total devem ser maiores que zero.");
            }
            if (req.parcelaAtual() > req.parcelaTotal()) {
                throw new IllegalArgumentException("Parcela atual nao pode ser maior que o total de parcelas.");
            }
            if (req.valorParcela() == null || req.valorParcela().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Informe um valor valido para cada parcela.");
            }
            if (req.valorParcela().multiply(BigDecimal.valueOf(req.parcelaTotal())).compareTo(req.valor()) > 0) {
                throw new IllegalArgumentException("A soma das parcelas nao pode ser maior que o valor total da obra.");
            }
            g.setParcelaAtual(req.parcelaAtual());
            g.setParcelaTotal(req.parcelaTotal());
        } else {
            g.setParcelaAtual(null);
            g.setParcelaTotal(null);
        }
        g.setObservacoes(SecurityUtils.blankToNull(req.observacoes()));
    }
}
