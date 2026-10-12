package br.com.sindico.app.ia;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoIARepository extends JpaRepository<ConfiguracaoIA, UUID> {
    Optional<ConfiguracaoIA> findByCondominioId(UUID condominioId);
}
