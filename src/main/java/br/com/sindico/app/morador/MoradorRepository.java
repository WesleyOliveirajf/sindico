package br.com.sindico.app.morador;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MoradorRepository extends JpaRepository<Morador, UUID> {

    @Query(
            "SELECT m FROM Morador m JOIN FETCH m.unidade u WHERE u.condominioId = :cid AND m.ativo = true ORDER BY "
                    + "u.bloco ASC, u.numero ASC, m.nome ASC")
    List<Morador> listarAtivosPorCondominio(@Param("cid") UUID condominioId);
}
