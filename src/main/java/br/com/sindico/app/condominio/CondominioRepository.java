package br.com.sindico.app.condominio;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CondominioRepository extends JpaRepository<Condominio, UUID> {
    Optional<Condominio> findByNome(String nome);
}