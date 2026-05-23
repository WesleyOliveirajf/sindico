package br.com.sindico.app.compromisso;

import br.com.sindico.app.security.TenantAccessor;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompromissoService {

    private final CompromissoRepository compromissoRepository;
    private final TenantAccessor tenantAccessor;

    public CompromissoService(CompromissoRepository compromissoRepository, TenantAccessor tenantAccessor) {
        this.compromissoRepository = compromissoRepository;
        this.tenantAccessor = tenantAccessor;
    }

    // ── Dashboard ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Compromisso> proximos() {
        return compromissoRepository
                .findTop10ByCondominioIdAndInicioEmGreaterThanEqualOrderByInicioEmAsc(
                        tenantAccessor.condominioAtual(), LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public long totalManutencoesAgendadas() {
        return compromissoRepository.countByCondominioIdAndTipo(
                tenantAccessor.condominioAtual(), CompromissoTipo.MANUTENCAO);
    }

    @Transactional(readOnly = true)
    public long totalReunioesAgendadas() {
        return compromissoRepository.countByCondominioIdAndTipo(
                tenantAccessor.condominioAtual(), CompromissoTipo.REUNIAO);
    }

    @Transactional(readOnly = true)
    public long totalPendencias() {
        return compromissoRepository.countByCondominioIdAndStatusNot(
                tenantAccessor.condominioAtual(), CompromissoStatus.CONCLUIDO);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Compromisso> listar() {
        return compromissoRepository
                .findByCondominioIdOrderByInicioEmDesc(tenantAccessor.condominioAtual());
    }

    @Transactional
    public Compromisso criar(NovoCompromissoForm form) {
        var c = new Compromisso();
        c.setTitulo(form.getTitulo());
        c.setDescricao(form.getDescricao());
        c.setTipo(form.getTipo() != null ? form.getTipo() : CompromissoTipo.OUTROS);
        c.setInicioEm(form.getInicioEm());
        c.setFimEm(form.getFimEm()); // pode ser null
        c.setLocal(form.getLocal());
        c.setStatus(CompromissoStatus.AGENDADO);
        c.setCondominioId(tenantAccessor.condominioAtual());
        return compromissoRepository.save(c);
    }

    /**
     * Marca o compromisso como CONCLUIDO e preenche fimEm com o momento atual.
     */
    @Transactional
    public Compromisso concluir(UUID id) {
        Compromisso c = compromissoRepository.findById(id)
                .filter(x -> x.getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new EntityNotFoundException("Lembrete nao encontrado."));
        c.setStatus(CompromissoStatus.CONCLUIDO);
        c.setFimEm(LocalDateTime.now());
        return compromissoRepository.save(c);
    }

    @Transactional
    public void deletar(UUID id) {
        Compromisso c = compromissoRepository.findById(id)
                .filter(x -> x.getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new EntityNotFoundException("Lembrete nao encontrado."));
        compromissoRepository.delete(c);
    }
}
