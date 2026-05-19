package br.com.sindico.app.usuario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    @Query("SELECT u FROM Usuario u WHERE LOWER(TRIM(u.email)) = :emailNorm AND u.status = 'ativo'")
    Optional<Usuario> findAtivoPorEmailNormalizado(@Param("emailNorm") String emailNorm);

    @Query("SELECT u FROM Usuario u WHERE LOWER(TRIM(u.email)) = :emailNorm")
    Optional<Usuario> findByEmailNormalizado(@Param("emailNorm") String emailNorm);

    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE LOWER(TRIM(u.email)) = :emailNorm")
    boolean existsByEmailNormalizado(@Param("emailNorm") String emailNorm);

    List<Usuario> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.ultimoAcesso > :since")
    long countOnlineRecentemente(@Param("since") LocalDateTime since);
}
