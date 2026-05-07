package br.com.sindico.app.compromisso;

import br.com.sindico.app.security.TenantAccessor;
import java.time.LocalDateTime;
import java.util.List;
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

    @Transactional
    public Compromisso criar(NovoCompromissoForm form) {
        validarDatas(form.getInicioEm(), form.getFimEm());

        Compromisso compromisso = new Compromisso();
        compromisso.setTitulo(form.getTitulo());
        compromisso.setDescricao(form.getDescricao());
        compromisso.setTipo(form.getTipo());
        compromisso.setInicioEm(form.getInicioEm());
        compromisso.setFimEm(form.getFimEm());
        compromisso.setLocal(form.getLocal());
        compromisso.setStatus(CompromissoStatus.AGENDADO);
        compromisso.setCondominioId(tenantAccessor.condominioAtual());

        return compromissoRepository.save(compromisso);
    }

    @Transactional(readOnly = true)
    public List<Compromisso> proximos() {
        return compromissoRepository.findTop10ByCondominioIdAndInicioEmGreaterThanEqualOrderByInicioEmAsc(
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

    private void validarDatas(LocalDateTime inicioEm, LocalDateTime fimEm) {
        if (inicioEm == null || fimEm == null) {
            throw new IllegalArgumentException("Datas obrigatorias");
        }
        if (!fimEm.isAfter(inicioEm)) {
            throw new IllegalArgumentException("Data final deve ser maior que a data inicial");
        }
    }
}
