package br.com.sindico.app.anexo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnexoRepository extends JpaRepository<Anexo, UUID> {
    List<Anexo> findByCondominioIdAndEntidadeTipoAndEntidadeIdOrderByCreatedAtDesc(
            UUID condominioId,
            String entidadeTipo,
            UUID entidadeId);

    Optional<Anexo> findByIdAndCondominioId(UUID id, UUID condominioId);
}
