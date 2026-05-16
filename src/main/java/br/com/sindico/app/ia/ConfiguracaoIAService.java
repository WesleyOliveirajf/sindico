package br.com.sindico.app.ia;

import br.com.sindico.app.security.TenantAccessor;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfiguracaoIAService {

    private final ConfiguracaoIARepository repository;
    private final TenantAccessor tenantAccessor;
    private final EncryptionService encryptionService;

    public ConfiguracaoIAService(
            ConfiguracaoIARepository repository,
            TenantAccessor tenantAccessor,
            EncryptionService encryptionService) {
        this.repository = repository;
        this.tenantAccessor = tenantAccessor;
        this.encryptionService = encryptionService;
    }

    @Transactional(readOnly = true)
    public ConfiguracaoIAResponse obter() {
        return repository.findByCondominioId(tenantAccessor.condominioAtual())
                .map(ConfiguracaoIAResponse::from)
                .orElse(ConfiguracaoIAResponse.naoConfigurado());
    }

    @Transactional
    public ConfiguracaoIAResponse salvar(ConfiguracaoIARequest req) {
        UUID condominioId = tenantAccessor.condominioAtual();
        ConfiguracaoIA config = repository.findByCondominioId(condominioId)
                .orElseGet(() -> {
                    ConfiguracaoIA c = new ConfiguracaoIA();
                    c.setCondominioId(condominioId);
                    return c;
                });

        config.setProvider(req.provider());
        config.setModel(blankToNull(req.model()));
        config.setBaseUrl(blankToNull(req.baseUrl()));
        config.setAtivo(req.ativo());

        if (req.apiKey() != null && !req.apiKey().isBlank()) {
            config.setApiKeyEnc(encryptionService.encrypt(req.apiKey().trim()));
        }

        return ConfiguracaoIAResponse.from(repository.save(config));
    }

    public Optional<ConfiguracaoIA> obterEntidade() {
        return repository.findByCondominioId(tenantAccessor.condominioAtual());
    }

    public String decryptApiKey(ConfiguracaoIA config) {
        if (config.getApiKeyEnc() == null || config.getApiKeyEnc().isBlank()) {
            throw new IllegalStateException("Chave API nao configurada");
        }
        return encryptionService.decrypt(config.getApiKeyEnc());
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}