package br.com.sindico.app.prestador;

import br.com.sindico.app.condominio.CondominioRepository;
import br.com.sindico.app.security.TenantAccessor;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrestadorServicoService {

    private final PrestadorServicoRepository prestadorServicoRepository;
    private final CondominioRepository condominioRepository;
    private final TenantAccessor tenantAccessor;

    public PrestadorServicoService(
            PrestadorServicoRepository prestadorServicoRepository,
            CondominioRepository condominioRepository,
            TenantAccessor tenantAccessor) {
        this.prestadorServicoRepository = prestadorServicoRepository;
        this.condominioRepository = condominioRepository;
        this.tenantAccessor = tenantAccessor;
    }

    @Transactional(readOnly = true)
    public List<PrestadorServico> listarDoCondominioAtual() {
        return prestadorServicoRepository.findByCondominioIdAndAtivoTrueOrderByNomeAsc(tenantAccessor.condominioAtual());
    }

    @Transactional(readOnly = true)
    public String nomeCondominioAtual() {
        return condominioRepository
                .findById(tenantAccessor.condominioAtual())
                .map(c -> c.getNome())
                .orElse("Condominio");
    }

    @Transactional
    public PrestadorServico criar(NovoPrestadorForm form) {
        PrestadorServico prestador = new PrestadorServico();
        prestador.setCondominioId(tenantAccessor.condominioAtual());
        prestador.setNome(form.getNome().trim());
        prestador.setTelefone(form.getTelefone().trim());
        prestador.setHistoricoServicos(blankToNull(form.getHistoricoServicos()));
        return prestadorServicoRepository.save(prestador);
    }

    @Transactional
    public PrestadorServico atualizar(UUID prestadorId, AtualizarPrestadorForm form) {
        PrestadorServico prestador = prestadorServicoRepository
                .findByIdAndCondominioId(prestadorId, tenantAccessor.condominioAtual())
                .orElseThrow(() -> new EntityNotFoundException("Prestador nao encontrado."));

        prestador.setNome(form.getNome().trim());
        prestador.setTelefone(form.getTelefone().trim());
        prestador.setHistoricoServicos(blankToNull(form.getHistoricoServicos()));
        return prestadorServicoRepository.save(prestador);
    }

    @Transactional
    public void inativar(UUID prestadorId) {
        PrestadorServico prestador = prestadorServicoRepository
                .findByIdAndCondominioId(prestadorId, tenantAccessor.condominioAtual())
                .orElseThrow(() -> new EntityNotFoundException("Prestador nao encontrado."));

        prestador.setAtivo(false);
        prestadorServicoRepository.save(prestador);
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
