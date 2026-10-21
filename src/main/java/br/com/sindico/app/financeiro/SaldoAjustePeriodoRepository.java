package br.com.sindico.app.financeiro;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaldoAjustePeriodoRepository extends JpaRepository<SaldoAjustePeriodo, UUID> {

    Optional<SaldoAjustePeriodo> findByCondominioIdAndAnoAndMes(UUID condominioId, int ano, int mes);

    @Query("""
            SELECT COALESCE(SUM(s.valor), 0)
            FROM SaldoAjustePeriodo s
            WHERE s.condominioId = :condominioId
              AND (:ano IS NULL OR s.ano = :ano)
              AND (:mes IS NULL OR s.mes = :mes)
            """)
    BigDecimal somarAjustes(
            @Param("condominioId") UUID condominioId,
            @Param("ano") Integer ano,
            @Param("mes") Integer mes);
}
