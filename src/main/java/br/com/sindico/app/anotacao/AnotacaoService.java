package br.com.sindico.app.anotacao;

import br.com.sindico.app.condominio.CondominioRepository;
import br.com.sindico.app.security.TenantAccessor;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnotacaoService {

    private final AnotacaoRepository anotacaoRepository;
    private final CondominioRepository condominioRepository;
    private final TenantAccessor tenantAccessor;

    public AnotacaoService(
            AnotacaoRepository anotacaoRepository,
            CondominioRepository condominioRepository,
            TenantAccessor tenantAccessor) {
        this.anotacaoRepository = anotacaoRepository;
        this.condominioRepository = condominioRepository;
        this.tenantAccessor = tenantAccessor;
    }

    @Transactional(readOnly = true)
    public List<Anotacao> listarDoCondominioAtual() {
        return anotacaoRepository.findByCondominioIdOrderByCreatedAtDesc(tenantAccessor.condominioAtual());
    }

    @Transactional(readOnly = true)
    public List<Anotacao> listarComFiltros(String texto, LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data final deve ser maior ou igual a data inicial.");
        }

        String textoNormalizado = null;
        if (texto != null) {
            String limpo = texto.trim();
            if (!limpo.isEmpty()) {
                textoNormalizado = limpo;
            }
        }

        LocalDateTime inicio = dataInicio == null ? null : dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim == null ? null : dataFim.atTime(23, 59, 59);

        return anotacaoRepository.buscarComFiltros(
                tenantAccessor.condominioAtual(),
                textoNormalizado,
                inicio,
                fim);
    }

    @Transactional(readOnly = true)
    public String nomeCondominioAtual() {
        return condominioRepository
                .findById(tenantAccessor.condominioAtual())
                .map(c -> c.getNome())
                .orElse("Condominio");
    }

    @Transactional
    public Anotacao criar(NovaAnotacaoForm form) {
        Anotacao a = new Anotacao();
        a.setCondominioId(tenantAccessor.condominioAtual());
        a.setTitulo(form.getTitulo().trim());
        a.setCategoria(blankToNull(form.getCategoria()));
        a.setDescricao(blankToNull(form.getDescricao()));
        a.setReferencia(blankToNull(form.getReferencia()));
        a.setImportancia(form.getImportancia());
        return anotacaoRepository.save(a);
    }

    @Transactional
    public Anotacao atualizar(UUID id, AnotacaoRequest req) {
        Anotacao a = anotacaoRepository.findById(id)
                .filter(x -> x.getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new EntityNotFoundException("Anotacao nao encontrada."));
        a.setTitulo(req.titulo().trim());
        a.setCategoria(blankToNull(req.categoria()));
        a.setDescricao(blankToNull(req.descricao()));
        a.setReferencia(blankToNull(req.referencia()));
        a.setImportancia(req.importancia());
        return anotacaoRepository.save(a);
    }

    @Transactional
    public void deletar(UUID id) {
        Anotacao a = anotacaoRepository.findById(id)
                .filter(x -> x.getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new EntityNotFoundException("Anotacao nao encontrada."));
        anotacaoRepository.delete(a);
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
