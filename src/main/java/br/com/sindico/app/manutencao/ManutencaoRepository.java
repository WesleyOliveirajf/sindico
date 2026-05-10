package br.com.sindico.app.manutencao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManutencaoRepository extends JpaRepository<Manutencao, UUID> {
    List<Manutencao> findByCondominioIdOrderByCreatedAtDesc(UUID condominioId);
}
