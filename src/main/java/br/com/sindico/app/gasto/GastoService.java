package br.com.sindico.app.gasto;

import br.com.sindico.app.security.SecurityUtils;
import br.com.sindico.app.security.TenantAccessor;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GastoService {

    private final GastoRepository gastoRepository;
    private final TenantAccessor tenantAccessor;

    public GastoService(GastoRepository gastoRepository, TenantAccessor tenantAccessor) {
        this.gastoRepository = gastoRepository;
        this.tenantAccessor = tenantAccessor;
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
        return gastoRepository.save(g);
    }

    @Transactional
    public Gasto atualizar(UUID id, GastoRequest req) {
        Gasto g = gastoRepository.findById(id)
                .filter(x -> x.getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new EntityNotFoundException("Gasto nao encontrado."));
        apply(g, req);
        return gastoRepository.save(g);
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
        g.setDataGasto(req.dataGasto());
        g.setFixo(req.fixo());
        g.setObservacoes(SecurityUtils.blankToNull(req.observacoes()));
    }
}
