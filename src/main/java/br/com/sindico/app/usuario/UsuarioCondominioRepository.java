package br.com.sindico.app.usuario;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioCondominioRepository extends JpaRepository<UsuarioCondominio, UUID> {

    @EntityGraph(attributePaths = "condominio")
    List<UsuarioCondominio> findByUsuario_IdOrderByCondominio_NomeAsc(UUID usuarioId);
}
