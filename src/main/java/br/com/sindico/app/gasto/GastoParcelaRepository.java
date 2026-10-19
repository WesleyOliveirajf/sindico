package br.com.sindico.app.gasto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GastoParcelaRepository extends JpaRepository<GastoParcela, UUID> {
    List<GastoParcela> findByGastoIdOrderByNumero(UUID gastoId);
    Optional<GastoParcela> findByGastoIdAndNumero(UUID gastoId, Integer numero);
    void deleteByGastoId(UUID gastoId);
}
