package br.com.sindico.app.recebimento;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecebimentoRepository extends JpaRepository<Recebimento, UUID> {

    List<Recebimento> findByCondominioIdOrderByDataRecebimentoDesc(UUID condominioId);

    @Query("""
            SELECT r FROM Recebimento r
            WHERE r.condominioId = :condominioId
              AND (:mes IS NULL OR MONTH(r.dataRecebimento) = :mes)
              AND (:ano IS NULL OR YEAR(r.dataRecebimento)  = :ano)
              AND (:tipo IS NULL OR r.tipo = :tipo)
            ORDER BY r.dataRecebimento DESC
            """)
    List<Recebimento> filtrar(
            @Param("condominioId") UUID condominioId,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano,
            @Param("tipo") RecebimentoTipo tipo);
}
