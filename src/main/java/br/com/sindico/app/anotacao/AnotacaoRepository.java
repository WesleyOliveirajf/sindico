package br.com.sindico.app.anotacao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnotacaoRepository extends JpaRepository<Anotacao, UUID> {

    List<Anotacao> findByCondominioIdOrderByCreatedAtDesc(UUID condominioId);
}
