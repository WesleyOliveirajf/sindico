package br.com.sindico.app.prestador;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrestadorServicoRepository extends JpaRepository<PrestadorServico, UUID> {

    List<PrestadorServico> findByCondominioIdAndAtivoTrueOrderByNomeAsc(UUID condominioId);

    Optional<PrestadorServico> findByIdAndCondominioId(UUID id, UUID condominioId);
}
