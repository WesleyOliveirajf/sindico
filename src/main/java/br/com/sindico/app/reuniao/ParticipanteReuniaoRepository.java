package br.com.sindico.app.reuniao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipanteReuniaoRepository extends JpaRepository<ParticipanteReuniao, UUID> {
    List<ParticipanteReuniao> findByReuniao_Id(UUID reuniaoId);
    void deleteByReuniao_Id(UUID reuniaoId);
}
