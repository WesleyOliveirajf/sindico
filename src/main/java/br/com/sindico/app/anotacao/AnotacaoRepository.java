package br.com.sindico.app.anotacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnotacaoRepository extends JpaRepository<Anotacao, UUID> {

    List<Anotacao> findByCondominioIdOrderByCreatedAtDesc(UUID condominioId);

    // Nota: :texto nunca e null — o service passa "" quando nao ha filtro.
    // Isso evita o PSQLException "could not determine data type of parameter"
    // que ocorre no Hibernate 6 + PostgreSQL quando um parametro String e null
    // numa clausula IS NULL.
    @Query("""
            SELECT a
            FROM Anotacao a
            WHERE a.condominioId = :condominioId
              AND (:texto = '' OR (
                    LOWER(a.titulo) LIKE LOWER(CONCAT('%', :texto, '%'))
                 OR LOWER(COALESCE(a.categoria, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
                 OR LOWER(COALESCE(a.descricao, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
                 OR LOWER(COALESCE(a.referencia, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
              ))
              AND (:inicio IS NULL OR a.createdAt >= :inicio)
              AND (:fim IS NULL OR a.createdAt <= :fim)
            ORDER BY a.createdAt DESC
            """)
    List<Anotacao> buscarComFiltros(
            @Param("condominioId") UUID condominioId,
            @Param("texto") String texto,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}
