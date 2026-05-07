package br.com.sindico.app.morador;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeRepository extends JpaRepository<Unidade, UUID> {

    List<Unidade> findByCondominioIdOrderByBlocoAscNumeroAsc(UUID condominioId);
}
