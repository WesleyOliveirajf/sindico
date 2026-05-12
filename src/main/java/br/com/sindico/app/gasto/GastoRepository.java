package br.com.sindico.app.gasto;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GastoRepository extends JpaRepository<Gasto, UUID> {
    List<Gasto> findByCondominioIdOrderByDataGastoDesc(UUID condominioId);
}
