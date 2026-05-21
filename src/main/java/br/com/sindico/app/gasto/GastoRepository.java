package br.com.sindico.app.gasto;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GastoRepository extends JpaRepository<Gasto, UUID> {

    List<Gasto> findByCondominioIdOrderByDataGastoDesc(UUID condominioId);

    /**
     * Filtra gastos diretamente no banco com suporte a parâmetros opcionais (mes, ano, tipo).
     * Evita carregar todo o histórico em memória (BUG-001 corrigido).
     * Usa YEAR() e MONTH() — funções HQL nativas do Hibernate para LocalDate.
     */
    @Query("""
            SELECT g FROM Gasto g
            WHERE g.condominioId = :condominioId
              AND (:mes IS NULL OR MONTH(g.dataGasto) = :mes)
              AND (:ano IS NULL OR YEAR(g.dataGasto)  = :ano)
              AND (:tipo IS NULL OR g.tipo = :tipo)
            ORDER BY g.dataGasto DESC
            """)
    List<Gasto> filtrar(
            @Param("condominioId") UUID condominioId,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano,
            @Param("tipo") GastoTipo tipo);
}
