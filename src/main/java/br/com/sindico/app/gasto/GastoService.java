package br.com.sindico.app.gasto;

import br.com.sindico.app.security.TenantAccessor;
import br.com.sindico.app.security.UsuarioTenantPrincipal;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        List<Gasto> todos = gastoRepository.findByCondominioIdOrderByDataGastoDesc(tenantAccessor.condominioAtual());
        return todos.stream()
                .filter(g -> mes == null || (g.getDataGasto() != null && g.getDataGasto().getMonthValue() == mes))
                .filter(g -> ano == null || (g.getDataGasto() != null && g.getDataGasto().getYear() == ano))
                .filter(g -> tipo == null || g.getTipo() == tipo)
                .toList();
    }

    @Transactional
    public Gasto criar(GastoRequest req) {
        Gasto g = new Gasto();
        apply(g, req);
        g.setCondominioId(tenantAccessor.condominioAtual());
        g.setCriadoPor(usuarioAtualId());
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
        g.setObservacoes(blankToNull(req.observacoes()));
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private UUID usuarioAtualId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioTenantPrincipal principal) {
            return principal.getUsuarioId();
        }
        throw new IllegalStateException("Nao foi possivel identificar usuario autenticado");
    }
}
