package br.com.sindico.app.anotacao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AnotacaoRepository extends JpaRepository<Anotacao, UUID>,
        JpaSpecificationExecutor<Anotacao> {

    List<Anotacao> findByCondominioIdOrderByCreatedAtDesc(UUID condominioId);
}
