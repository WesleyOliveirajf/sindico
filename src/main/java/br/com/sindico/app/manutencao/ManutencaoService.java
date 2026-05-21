package br.com.sindico.app.manutencao;

import br.com.sindico.app.security.SecurityUtils;
import br.com.sindico.app.security.TenantAccessor;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManutencaoService {

    private final ManutencaoRepository manutencaoRepository;
    private final TenantAccessor tenantAccessor;

    public ManutencaoService(ManutencaoRepository manutencaoRepository, TenantAccessor tenantAccessor) {
        this.manutencaoRepository = manutencaoRepository;
        this.tenantAccessor = tenantAccessor;
    }

    @Transactional(readOnly = true)
    public List<Manutencao> listarDoCondominioAtual() {
        return manutencaoRepository.findByCondominioIdOrderByCreatedAtDesc(tenantAccessor.condominioAtual());
    }

    @Transactional
    public Manutencao criar(ManutencaoRequest req) {
        Manutencao m = new Manutencao();
        apply(m, req);
        m.setCondominioId(tenantAccessor.condominioAtual());
        m.setCriadoPor(SecurityUtils.usuarioAtualId());
        return manutencaoRepository.save(m);
    }

    @Transactional
    public Manutencao atualizar(UUID id, ManutencaoRequest req) {
        Manutencao m = manutencaoRepository.findById(id)
                .filter(x -> x.getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new EntityNotFoundException("Manutencao nao encontrada."));
        apply(m, req);
        return manutencaoRepository.save(m);
    }

    @Transactional
    public void deletar(UUID id) {
        Manutencao m = manutencaoRepository.findById(id)
                .filter(x -> x.getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new EntityNotFoundException("Manutencao nao encontrada."));
        manutencaoRepository.delete(m);
    }

    private static void apply(Manutencao m, ManutencaoRequest req) {
        m.setTitulo(req.titulo().trim());
        m.setDescricao(SecurityUtils.blankToNull(req.descricao()));
        m.setTipo(req.tipo());
        m.setCategoria(SecurityUtils.blankToNull(req.categoria()));
        m.setLocal(SecurityUtils.blankToNull(req.local()));
        m.setAtivoId(req.ativoId());
        m.setFornecedorId(req.fornecedorId());
        m.setResponsavelInterno(SecurityUtils.blankToNull(req.responsavelInterno()));
        m.setDataOcorrencia(req.dataOcorrencia());
        m.setDataExecucao(req.dataExecucao());
        m.setCustoPrevisto(req.custoPrevisto());
        m.setCustoRealizado(req.custoRealizado());
        m.setStatus(req.status());
        m.setObservacoes(SecurityUtils.blankToNull(req.observacoes()));
    }
}
