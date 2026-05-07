package br.com.sindico.app.usuario;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SenhaResetTokenRepository extends JpaRepository<SenhaResetToken, UUID> {

    Optional<SenhaResetToken> findByToken(String token);

    /** Invalida tokens anteriores do mesmo usuario (nao usado + ainda valido) antes de gerar novo. */
    @Modifying
    @Query("UPDATE SenhaResetToken t SET t.usado = true WHERE t.usuario.id = :uid AND t.usado = false")
    void invalidarTokensDoUsuario(@Param("uid") UUID usuarioId);
}
