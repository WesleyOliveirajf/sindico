package br.com.sindico.app.compromisso;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CompromissoRepository extends JpaRepository<Compromisso, UUID> {

    @Query("SELECT c FROM Compromisso c WHERE c.inicioEm >= :agora ORDER BY c.inicioEm ASC LIMIT 10")
    List<Compromisso> findProximos(@Param("agora") LocalDateTime agora);

    @Query("SELECT COUNT(c) FROM Compromisso c WHERE c.tipo = :tipo")
    long countByTipo(CompromissoTipo tipo);

    @Query("SELECT COUNT(c) FROM Compromisso c WHERE c.status <> :status")
    long countByStatusNot(CompromissoStatus status);
}
