package br.com.sindico.app.compromisso;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CompromissoRepository extends JpaRepository<Compromisso, UUID> {

    List<Compromisso> findTop10ByCondominioIdAndInicioEmGreaterThanEqualOrderByInicioEmAsc(
            UUID condominioId, LocalDateTime inicioEmMinimo);

    List<Compromisso> findByCondominioIdOrderByInicioEmDesc(UUID condominioId);

    long countByCondominioIdAndTipo(UUID condominioId, CompromissoTipo tipo);

    long countByCondominioIdAndStatusNot(UUID condominioId, CompromissoStatus status);
}
