package br.com.sindico.app.condominio;

import br.com.sindico.app.security.TenantAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CondominioService {

    private final CondominioRepository condominioRepository;
    private final TenantAccessor tenantAccessor;

    public CondominioService(CondominioRepository condominioRepository, TenantAccessor tenantAccessor) {
        this.condominioRepository = condominioRepository;
        this.tenantAccessor = tenantAccessor;
    }

    @Transactional(readOnly = true)
    public CondominioForm buscarFormAtual() {
        Condominio condominio = buscarAtual();

        CondominioForm form = new CondominioForm();
        form.setNome(condominio.getNome());
        form.setCnpj(condominio.getCnpj());
        form.setEndereco(condominio.getEndereco());
        return form;
    }

    @Transactional
    public Condominio atualizar(CondominioForm form) {
        Condominio condominio = buscarAtual();
        condominio.setNome(requiredTrim(form.getNome(), "Nome do condominio e obrigatorio"));
        condominio.setCnpj(blankToNull(form.getCnpj()));
        condominio.setEndereco(blankToNull(form.getEndereco()));
        return condominioRepository.save(condominio);
    }

    private Condominio buscarAtual() {
        return condominioRepository
                .findById(tenantAccessor.condominioAtual())
                .orElseThrow(() -> new IllegalStateException("Condominio nao encontrado"));
    }

    private static String requiredTrim(String value, String message) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
