package br.com.sindico.app.recebimento;

import br.com.sindico.app.security.SecurityUtils;
import br.com.sindico.app.security.TenantAccessor;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecebimentoService {

    private final RecebimentoRepository recebimentoRepository;
    private final TenantAccessor tenantAccessor;

    public RecebimentoService(RecebimentoRepository recebimentoRepository, TenantAccessor tenantAccessor) {
        this.recebimentoRepository = recebimentoRepository;
        this.tenantAccessor = tenantAccessor;
    }

    @Transactional(readOnly = true)
    public List<Recebimento> listar(Integer mes, Integer ano, RecebimentoTipo tipo) {
        return recebimentoRepository.filtrar(tenantAccessor.condominioAtual(), mes, ano, tipo);
    }

    @Transactional
    public Recebimento criar(RecebimentoRequest req) {
        Recebimento r = new Recebimento();
        apply(r, req);
        r.setCondominioId(tenantAccessor.condominioAtual());
        r.setCriadoPor(SecurityUtils.usuarioAtualId());
        return recebimentoRepository.save(r);
    }

    @Transactional
    public void deletar(UUID id) {
        Recebimento r = recebimentoRepository.findById(id)
                .filter(x -> x.getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new EntityNotFoundException("Recebimento nao encontrado."));
        recebimentoRepository.delete(r);
    }

    private static void apply(Recebimento r, RecebimentoRequest req) {
        r.setDescricao(req.descricao().trim());
        r.setTipo(req.tipo());
        r.setValor(req.valor());
        r.setDataRecebimento(req.dataRecebimento());
        r.setObservacoes(SecurityUtils.blankToNull(req.observacoes()));
    }
}
