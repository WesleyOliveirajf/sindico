package br.com.sindico.app.usuario;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioConsentimentoRepository extends JpaRepository<UsuarioConsentimento, UUID> {
    List<UsuarioConsentimento> findByUsuarioIdOrderByAcceptedAtDesc(UUID usuarioId);
}
