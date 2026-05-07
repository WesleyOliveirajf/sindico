package br.com.sindico.app.morador;

import br.com.sindico.app.condominio.CondominioRepository;
import br.com.sindico.app.security.TenantAccessor;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoradorGestaoService {

    private final UnidadeRepository unidadeRepository;
    private final MoradorRepository moradorRepository;
    private final CondominioRepository condominioRepository;
    private final TenantAccessor tenantAccessor;

    public MoradorGestaoService(
            UnidadeRepository unidadeRepository,
            MoradorRepository moradorRepository,
            CondominioRepository condominioRepository,
            TenantAccessor tenantAccessor) {
        this.unidadeRepository = unidadeRepository;
        this.moradorRepository = moradorRepository;
        this.condominioRepository = condominioRepository;
        this.tenantAccessor = tenantAccessor;
    }

    @Transactional(readOnly = true)
    public String nomeCondominioAtual() {
        return condominioRepository
                .findById(tenantAccessor.condominioAtual())
                .map(c -> c.getNome())
                .orElse("Condominio");
    }

    @Transactional(readOnly = true)
    public List<Unidade> listarUnidades() {
        return unidadeRepository.findByCondominioIdOrderByBlocoAscNumeroAsc(tenantAccessor.condominioAtual());
    }

    @Transactional(readOnly = true)
    public List<Morador> listarMoradoresAtivos() {
        return moradorRepository.listarAtivosPorCondominio(tenantAccessor.condominioAtual());
    }

    @Transactional
    public void criarUnidade(NovaUnidadeForm form) {
        Unidade u = new Unidade();
        u.setCondominioId(tenantAccessor.condominioAtual());
        u.setBloco(form.getBloco() == null ? "" : form.getBloco().trim());
        u.setNumero(form.getNumero().trim());
        u.setComplemento(blankToNull(form.getComplemento()));
        try {
            unidadeRepository.save(u);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Ja existe unidade com este bloco e numero.");
        }
    }

    @Transactional
    public void criarMorador(NovoMoradorForm form) {
        Unidade unidade =
                unidadeRepository.findById(form.getUnidadeId()).orElseThrow(() -> new IllegalArgumentException("Unidade nao encontrada."));
        if (!unidade.getCondominioId().equals(tenantAccessor.condominioAtual())) {
            throw new IllegalArgumentException("Unidade nao pertence a este condominio.");
        }
        Morador m = new Morador();
        m.setUnidade(unidade);
        m.setNome(form.getNome().trim());
        m.setEmail(blankToNull(form.getEmail()));
        m.setTelefone(blankToNull(form.getTelefone()));
        m.setPapel(form.getPapel());
        m.setObservacoes(blankToNull(form.getObservacoes()));
        m.setAtivo(true);
        moradorRepository.save(m);
    }

    @Transactional
    public Morador atualizarMorador(UUID moradorId, MoradorRequest req) {
        Morador m = moradorRepository.findById(moradorId)
                .filter(x -> x.getUnidade().getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new EntityNotFoundException("Morador nao encontrado."));
        Unidade unidade = unidadeRepository.findById(req.unidadeId())
                .filter(u -> u.getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new IllegalArgumentException("Unidade nao encontrada."));
        m.setUnidade(unidade);
        m.setNome(req.nome().trim());
        m.setEmail(blankToNull(req.email()));
        m.setTelefone(blankToNull(req.telefone()));
        m.setPapel(req.papel());
        m.setObservacoes(blankToNull(req.observacoes()));
        return moradorRepository.save(m);
    }

    @Transactional
    public void inativarMorador(UUID moradorId) {
        Morador m = moradorRepository.findById(moradorId)
                .filter(x -> x.getUnidade().getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new EntityNotFoundException("Morador nao encontrado."));
        m.setAtivo(false);
        moradorRepository.save(m);
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
