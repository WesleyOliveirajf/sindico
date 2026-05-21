package br.com.sindico.app.usuario;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioCondominioRepository extends JpaRepository<UsuarioCondominio, UUID> {

    @EntityGraph(attributePaths = "condominio")
    List<UsuarioCondominio> findByUsuario_IdOrderByCondominio_NomeAsc(UUID usuarioId);

    /**
     * Carrega vínculos de todos os usuários informados em uma única query (JOIN FETCH).
     * Evita o problema N+1 no painel admin (BUG-002 corrigido).
     */
    @Query("SELECT uc FROM UsuarioCondominio uc JOIN FETCH uc.condominio WHERE uc.usuario.id IN :usuarioIds")
    List<UsuarioCondominio> findByUsuarioIdsComCondominio(@org.springframework.data.repository.query.Param("usuarioIds") List<UUID> usuarioIds);
}
