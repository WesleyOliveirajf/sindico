package br.com.sindico.app.reuniao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReuniaoRepository extends JpaRepository<Reuniao, UUID> {
    List<Reuniao> findByCondominioIdOrderByDataHoraDesc(UUID condominioId);
}
