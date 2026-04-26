package br.com.sindico.app.compromisso;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompromissoService {

    private final CompromissoRepository compromissoRepository;

    public CompromissoService(CompromissoRepository compromissoRepository) {
        this.compromissoRepository = compromissoRepository;
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

        return compromissoRepository.save(compromisso);
    }

    @Transactional(readOnly = true)
    public List<Compromisso> proximos() {
        return compromissoRepository.findTop10ByOrderByInicioEmAsc();
    }

    @Transactional(readOnly = true)
    public long totalManutencoesAgendadas() {
        return compromissoRepository.countByTipo(CompromissoTipo.MANUTENCAO);
    }

    @Transactional(readOnly = true)
    public long totalReunioesAgendadas() {
        return compromissoRepository.countByTipo(CompromissoTipo.REUNIAO);
    }

    @Transactional(readOnly = true)
    public long totalPendencias() {
        return compromissoRepository.countByStatusNot(CompromissoStatus.CONCLUIDO);
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
