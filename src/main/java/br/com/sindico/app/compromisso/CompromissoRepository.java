package br.com.sindico.app.compromisso;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CompromissoRepository extends JpaRepository<Compromisso, UUID> {

    List<Compromisso> findTop10ByOrderByInicioEmAsc();

    @Query("SELECT COUNT(c) FROM Compromisso c WHERE c.tipo = :tipo")
    long countByTipo(CompromissoTipo tipo);

    @Query("SELECT COUNT(c) FROM Compromisso c WHERE c.status <> :status")
    long countByStatusNot(CompromissoStatus status);
}
